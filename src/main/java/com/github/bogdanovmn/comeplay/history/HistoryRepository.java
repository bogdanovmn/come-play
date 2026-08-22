package com.github.bogdanovmn.comeplay.history;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
class HistoryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    HistoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void recordVisit(UUID clubId, UUID userId, LocalDate slotDate, String sportType) {
        jdbc.update("""
                INSERT INTO visit_history (id, club_id, user_id, slot_date, sport_type, recorded_at)
                VALUES (:id, :clubId, :userId, :slotDate, :sportType, :recordedAt)
                ON CONFLICT DO NOTHING
                """,
                Map.of(
                        "id", UUID.randomUUID(),
                        "clubId", clubId,
                        "userId", userId,
                        "slotDate", slotDate,
                        "sportType", sportType,
                        "recordedAt", Instant.now()
                )
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
