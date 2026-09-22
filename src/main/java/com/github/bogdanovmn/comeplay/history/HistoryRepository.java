package com.github.bogdanovmn.comeplay.history;

import com.github.bogdanovmn.comeplay.common.TrainingSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class HistoryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    void recordVisit(UUID clubId, UUID userId, LocalDate slotDate, String sportType) {
        jdbc.update("""
                INSERT INTO visit_history (club_id, user_id, slot_date, sport_type)
                VALUES (:clubId, :userId, :slotDate, :sportType)
                ON CONFLICT DO NOTHING
                """,
                Map.of(
                        "clubId", clubId,
                        "userId", userId,
                        "slotDate", slotDate,
                        "sportType", sportType
                )
        );
    }

    List<TrainingSlot> pastTrainings(UUID clubId, LocalDate from, LocalDate to, UUID viewerId) {
        return jdbc.query("""
                %s
                WHERE t.club_id = :clubId
                    AND ts.slot_date >= :from AND ts.slot_date <= :to
                ORDER BY ts.slot_date DESC, COALESCE(ts.start_time, t.start_time) DESC
                """.formatted(TrainingSlot.SELECT),
                new MapSqlParameterSource()
                        .addValue("clubId", clubId)
                        .addValue("from", from)
                        .addValue("to", to)
                        .addValue("viewerId", viewerId),
                TrainingSlot.ROW_MAPPER
        );
    }

    List<VisitByDay> visitSummaryByDay(UUID clubId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT slot_date, COUNT(*) AS visit_count
                FROM visit_history
                WHERE club_id = :clubId AND slot_date >= :from AND slot_date <= :to
                GROUP BY slot_date
                ORDER BY slot_date
                """,
                Map.of("clubId", clubId, "from", from, "to", to),
                (rs, rowNum) -> VisitByDay.builder()
                        .date(rs.getDate("slot_date").toLocalDate())
                        .visitCount(rs.getInt("visit_count"))
                        .build()
        );
    }

    List<VisitByPlayer> visitSummaryByPlayer(UUID clubId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT user_id, COUNT(*) AS visit_count
                FROM visit_history
                WHERE club_id = :clubId AND slot_date >= :from AND slot_date <= :to
                GROUP BY user_id
                ORDER BY visit_count DESC
                """,
                Map.of("clubId", clubId, "from", from, "to", to),
                (rs, rowNum) -> VisitByPlayer.builder()
                        .userId(UUID.fromString(rs.getString("user_id")))
                        .visitCount(rs.getInt("visit_count"))
                        .build()
        );
    }
}
