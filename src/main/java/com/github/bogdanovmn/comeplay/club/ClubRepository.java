package com.github.bogdanovmn.comeplay.club;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.sql.Timestamp;
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
            .ownerId(UUID.fromString(rs.getString("owner_id")))
            .closed(rs.getBoolean("closed"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .build();

    private static final RowMapper<ClubBrief> CLUB_BRIEF_ROW_MAPPER = (rs, rowNum) -> ClubBrief.builder()
            .id(UUID.fromString(rs.getString("id")))
            .name(rs.getString("name"))
            .membersCount(rs.getInt("members_count"))
            .build();

    private static final RowMapper<InvitationBrief> INVITATION_BRIEF_ROW_MAPPER = (rs, rowNum) -> InvitationBrief.builder()
            .id(UUID.fromString(rs.getString("id")))
            .name(rs.getString("name"))
            .joinedCount(rs.getInt("joined_count"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;


    List<ClubBrief> listByOwner(UUID userId) {
        return jdbc.query("""
                SELECT c.id, c.name,
                    (SELECT COUNT(*) FROM club_member cm WHERE cm.club_id = c.id) AS members_count
                FROM club c
                WHERE c.owner_id = :userId
                ORDER BY c.name
                """,
                Map.of("userId", userId),
                CLUB_BRIEF_ROW_MAPPER
        );
    }

    List<ClubBrief> listByMember(UUID userId) {
        return jdbc.query("""
                SELECT c.id, c.name,
                    (SELECT COUNT(*) FROM club_member cm WHERE cm.club_id = c.id) AS members_count
                FROM club c
                JOIN club_member cm ON cm.club_id = c.id
                WHERE cm.user_id = :userId AND c.closed = false
                ORDER BY c.name
                """,
                Map.of("userId", userId),
                CLUB_BRIEF_ROW_MAPPER
        );
    }

    Optional<Club> findById(UUID clubId) {
        var result = jdbc.query("""
                SELECT id, name, owner_id, closed, created_at
                FROM club
                WHERE id = :clubId
                """,
                Map.of("clubId", clubId),
                CLUB_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    UUID create(String name, UUID ownerId) {
        return jdbc.queryForObject("""
                INSERT INTO club (name, owner_id, closed, created_at)
                VALUES (:name, :ownerId, false, :createdAt)
                RETURNING id
                """,
                Map.of(
                        "name", name,
                        "ownerId", ownerId,
                        "createdAt", Timestamp.from(Instant.now())
                ),
                UUID.class
        );
    }

    void update(UUID clubId, String name) {
        jdbc.update("""
                UPDATE club SET name = :name WHERE id = :clubId
                """,
                Map.of("clubId", clubId, "name", name)
        );
    }

    void close(UUID clubId) {
        jdbc.update("""
                UPDATE club SET closed = true WHERE id = :clubId
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

    List<InvitationBrief> listInvitations(UUID clubId) {
        return jdbc.query("""
                SELECT i.id, i.name,
                    (SELECT COUNT(*) FROM club_member cm WHERE cm.club_id = :clubId) AS joined_count
                FROM invitation i
                WHERE i.club_id = :clubId
                ORDER BY i.created_at
                """,
                Map.of("clubId", clubId),
                INVITATION_BRIEF_ROW_MAPPER
        );
    }

    Optional<Invitation> findInvitationById(UUID invitationId) {
        var result = jdbc.query("""
                SELECT id, club_id, name, created_by, created_at
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
                        .build()
        );
        return result.stream().findFirst();
    }

    UUID createInvitation(UUID clubId, String name, UUID createdBy) {
        return jdbc.queryForObject("""
                INSERT INTO invitation (club_id, name, created_by, created_at)
                VALUES (:clubId, :name, :createdBy, :createdAt)
                RETURNING id
                """,
                Map.of(
                        "clubId", clubId,
                        "name", name,
                        "createdBy", createdBy,
                        "createdAt", Timestamp.from(Instant.now())
                ),
                UUID.class
        );
    }
}
