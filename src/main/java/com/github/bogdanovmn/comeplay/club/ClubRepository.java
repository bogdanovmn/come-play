package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.common.SkillLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class ClubRepository {

    private static final RowMapper<Club> CLUB_ROW_MAPPER = (rs, rowNum) -> Club.builder()
            .id(UUID.fromString(rs.getString("id")))
            .name(rs.getString("name"))
            .sportTypeId(rs.getInt("sport_type_id"))
            .sportTypeName(rs.getString("sport_type_name"))
            .ownerId(UUID.fromString(rs.getString("owner_id")))
            .ownerName(rs.getString("owner_name"))
            .description(rs.getString("description"))
            .closed(rs.getBoolean("closed"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .build();

    private static final RowMapper<ClubBrief> CLUB_BRIEF_ROW_MAPPER = (rs, rowNum) -> ClubBrief.builder()
            .id(UUID.fromString(rs.getString("id")))
            .name(rs.getString("name"))
            .sportTypeId(rs.getInt("sport_type_id"))
            .sportTypeName(rs.getString("sport_type_name"))
            .membersCount(rs.getInt("members_count"))
            .build();

    private static final RowMapper<InvitationBrief> INVITATION_BRIEF_ROW_MAPPER = (rs, rowNum) -> InvitationBrief.builder()
            .id(UUID.fromString(rs.getString("id")))
            .name(rs.getString("name"))
            .joinedCount(rs.getInt("joined_count"))
            .active(rs.getBoolean("active"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;


    List<ClubBrief> listByOwner(UUID userId) {
        return jdbc.query("""
                SELECT c.id, c.name, c.sport_type_id, st.name AS sport_type_name,
                    (SELECT COUNT(*) FROM club_member cm WHERE cm.club_id = c.id) AS members_count
                FROM club c
                JOIN sport_type st ON st.id = c.sport_type_id
                WHERE c.owner_id = :userId
                ORDER BY c.name
                """,
                Map.of("userId", userId),
                CLUB_BRIEF_ROW_MAPPER
        );
    }

    List<ClubBrief> listByMember(UUID userId) {
        return jdbc.query("""
                SELECT c.id, c.name, c.sport_type_id, st.name AS sport_type_name,
                    (SELECT COUNT(*) FROM club_member cm WHERE cm.club_id = c.id) AS members_count
                FROM club c
                JOIN club_member cm ON cm.club_id = c.id
                JOIN sport_type st ON st.id = c.sport_type_id
                WHERE cm.user_id = :userId AND c.closed = false
                ORDER BY c.name
                """,
                Map.of("userId", userId),
                CLUB_BRIEF_ROW_MAPPER
        );
    }

    Optional<Club> findById(UUID clubId) {
        var result = jdbc.query("""
                SELECT c.id, c.name, c.sport_type_id, st.name AS sport_type_name,
                    c.owner_id, u.display_name AS owner_name, c.description, c.closed, c.created_at
                FROM club c
                JOIN sport_type st ON st.id = c.sport_type_id
                JOIN app_user u ON u.id = c.owner_id
                WHERE c.id = :clubId
                """,
                Map.of("clubId", clubId),
                CLUB_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    UUID create(String name, int sportTypeId, String description, UUID ownerId) {
        return jdbc.queryForObject("""
                INSERT INTO club (name, sport_type_id, description, owner_id, closed)
                VALUES (:name, :sportTypeId, :description, :ownerId, false)
                RETURNING id
                """,
                new MapSqlParameterSource()
                    .addValue("name", name)
                    .addValue("sportTypeId", sportTypeId)
                    .addValue("description", description)
                    .addValue("ownerId", ownerId),
                UUID.class
        );
    }

    void update(UUID clubId, String name, int sportTypeId, String description) {
        jdbc.update("""
                UPDATE club SET name = :name, sport_type_id = :sportTypeId, description = :description
                WHERE id = :clubId
                """,
                Map.of("clubId", clubId, "name", name, "sportTypeId", sportTypeId, "description", description)
        );
    }

    void close(UUID clubId) {
        jdbc.update("""
                UPDATE club SET closed = true WHERE id = :clubId
                """,
                Map.of("clubId", clubId)
        );
    }

    void open(UUID clubId) {
        jdbc.update("""
                UPDATE club SET closed = false WHERE id = :clubId
                """,
                Map.of("clubId", clubId)
        );
    }

    boolean isOwner(UUID clubId, UUID userId) {
        var result = jdbc.queryForList("""
                SELECT 1 FROM club WHERE id = :clubId AND owner_id = :userId
                """,
                Map.of("clubId", clubId, "userId", userId)
        );
        return !result.isEmpty();
    }

    boolean isMember(UUID clubId, UUID userId) {
        var result = jdbc.queryForList("""
                SELECT 1 FROM club_member WHERE club_id = :clubId AND user_id = :userId
                """,
                Map.of("clubId", clubId, "userId", userId)
        );
        return !result.isEmpty();
    }

    void addMember(UUID clubId, UUID userId) {
        jdbc.update("""
                INSERT INTO club_member (club_id, user_id) VALUES (:clubId, :userId)
                ON CONFLICT DO NOTHING
                """,
                Map.of("clubId", clubId, "userId", userId)
        );
    }

    void removeMember(UUID clubId, UUID userId) {
        jdbc.update("""
                DELETE FROM club_member
                WHERE club_id = :clubId AND user_id = :userId
                """,
                Map.of("clubId", clubId, "userId", userId)
        );
    }

    List<ClubMember> listMembers(UUID clubId) {
        return jdbc.query("""
                SELECT u.id, u.display_name AS name,
                    c.owner_id = u.id AS is_owner,
                    cps.skill IS NOT NULL AS overridden,
                    COALESCE(cps.skill, ps.skill) AS skill
                FROM club_member cm
                JOIN app_user u ON u.id = cm.user_id
                JOIN club c ON c.id = cm.club_id
                LEFT JOIN club_player_skill cps ON cps.club_id = cm.club_id AND cps.user_id = u.id
                LEFT JOIN player_skill ps ON ps.user_id = u.id AND ps.sport_type_id = c.sport_type_id
                WHERE cm.club_id = :clubId
                ORDER BY u.display_name
                """,
                Map.of("clubId", clubId),
                (rs, rowNum) -> ClubMember.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .name(rs.getString("name"))
                        .skill(rs.getString("skill") != null ? SkillLevel.valueOf(rs.getString("skill")) : null)
                        .owner(rs.getBoolean("is_owner"))
                        .overridden(rs.getBoolean("overridden"))
                        .build()
        );
    }

    void softDeleteInvitation(UUID invitationId) {
        jdbc.update("""
                UPDATE invitation SET active = false WHERE id = :invitationId
                """,
                Map.of("invitationId", invitationId)
        );
    }

    List<InvitationBrief> listInvitations(UUID clubId) {
        return jdbc.query("""
                SELECT i.id, i.name, i.active,
                    (SELECT COUNT(*) FROM invitation_history ji WHERE ji.invitation_id = i.id) AS joined_count
                FROM invitation i
                WHERE i.club_id = :clubId
                ORDER BY i.created_at
                """,
                Map.of("clubId", clubId),
                INVITATION_BRIEF_ROW_MAPPER
        );
    }

    List<InvitationJoiner> listInvitationJoiners(UUID invitationId) {
        return jdbc.query("""
                SELECT u.id AS user_id, u.display_name AS name, ji.joined_at
                FROM invitation_history ji
                JOIN app_user u ON u.id = ji.user_id
                WHERE ji.invitation_id = :invitationId
                ORDER BY ji.joined_at
                """,
                Map.of("invitationId", invitationId),
                (rs, rowNum) -> InvitationJoiner.builder()
                        .userId(UUID.fromString(rs.getString("user_id")))
                        .name(rs.getString("name"))
                        .registeredAt(rs.getTimestamp("joined_at").toInstant())
                        .build()
        );
    }

    void recordJoiner(UUID invitationId, UUID userId) {
        jdbc.update("""
                INSERT INTO invitation_history (invitation_id, user_id)
                VALUES (:invitationId, :userId)
                ON CONFLICT DO NOTHING
                """,
                Map.of("invitationId", invitationId, "userId", userId)
        );
    }

    Optional<InvitationInfo> findInvitationInfoById(UUID invitationId) {
        var result = jdbc.query("""
                SELECT i.id, i.club_id, c.name AS club_name, i.name
                FROM invitation i
                JOIN club c ON c.id = i.club_id
                WHERE i.id = :invitationId AND i.active = true
                """,
                Map.of("invitationId", invitationId),
                (rs, rowNum) -> InvitationInfo.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .clubId(UUID.fromString(rs.getString("club_id")))
                        .clubName(rs.getString("club_name"))
                        .name(rs.getString("name"))
                        .build()
        );
        return result.stream().findFirst();
    }

    Optional<Invitation> findInvitationById(UUID invitationId) {
        var result = jdbc.query("""
                SELECT id, club_id, name, created_by, created_at, active
                FROM invitation
                WHERE id = :invitationId
                """,
                Map.of("invitationId", invitationId),
                (rs, rowNum) -> Invitation.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .clubId(UUID.fromString(rs.getString("club_id")))
                        .name(rs.getString("name"))
                        .createdBy(UUID.fromString(rs.getString("created_by")))
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .active(rs.getBoolean("active"))
                        .build()
        );
        return result.stream().findFirst();
    }

    UUID createInvitation(UUID clubId, String name, UUID createdBy) {
        return jdbc.queryForObject("""
                INSERT INTO invitation (club_id, name, created_by)
                VALUES (:clubId, :name, :createdBy)
                RETURNING id
                """,
                Map.of(
                        "clubId", clubId,
                        "name", name,
                        "createdBy", createdBy
                ),
                UUID.class
        );
    }
}
