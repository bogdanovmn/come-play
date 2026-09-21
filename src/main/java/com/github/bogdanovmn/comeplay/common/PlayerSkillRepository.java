package com.github.bogdanovmn.comeplay.common;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlayerSkillRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<PlayerSkill> userSkills(UUID userId) {
        return jdbc.query("""
                SELECT ps.sport_type_id, st.name AS sport_type_name, ps.skill
                FROM player_skill ps
                JOIN sport_type st ON st.id = ps.sport_type_id
                WHERE ps.user_id = :userId
                ORDER BY st.name
                """,
                Map.of("userId", userId),
                (rs, rowNum) -> PlayerSkill.builder()
                    .sportTypeId(rs.getInt("sport_type_id"))
                    .sportTypeName(rs.getString("sport_type_name"))
                    .skill(SkillLevel.valueOf(rs.getString("skill")))
                    .build()
        );
    }

    public void setUserSkill(UUID userId, int sportTypeId, SkillLevel skill) {
        jdbc.update("""
                INSERT INTO player_skill (user_id, sport_type_id, skill)
                VALUES (:userId, :sportTypeId, :skill)
                ON CONFLICT (user_id, sport_type_id)
                    DO UPDATE SET skill = EXCLUDED.skill
                """,
                new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("sportTypeId", sportTypeId)
                    .addValue("skill", skill.name())
        );
    }

    public void deleteUserSkill(UUID userId, int sportTypeId) {
        jdbc.update("""
                DELETE FROM player_skill
                WHERE user_id = :userId AND sport_type_id = :sportTypeId
                """,
                Map.of("userId", userId, "sportTypeId", sportTypeId)
        );
    }

    public void setClubOverride(UUID clubId, UUID userId, SkillLevel skill) {
        jdbc.update("""
                INSERT INTO club_player_skill (club_id, user_id, skill)
                VALUES (:clubId, :userId, :skill)
                ON CONFLICT (club_id, user_id)
                    DO UPDATE SET skill = EXCLUDED.skill
                """,
                new MapSqlParameterSource()
                    .addValue("clubId", clubId)
                    .addValue("userId", userId)
                    .addValue("skill", skill.name())
        );
    }

    public void deleteClubOverride(UUID clubId, UUID userId) {
        jdbc.update("""
                DELETE FROM club_player_skill
                WHERE club_id = :clubId AND user_id = :userId
                """,
                Map.of("clubId", clubId, "userId", userId)
        );
    }
}