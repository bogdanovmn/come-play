package com.github.bogdanovmn.comeplay.common;

import lombok.Builder;
import lombok.Value;
import org.springframework.jdbc.core.RowMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Value
@Builder
public class TrainingSlot {
    UUID id;
    UUID trainingId;
    UUID clubId;
    String clubName;
    LocalDate slotDate;
    DayOfWeek dayOfWeek;
    LocalTime startTime;
    LocalTime endTime;
    int enrolledCount;
    int maxPlayers;
    int commentsCount;
    String features;
    boolean overridden;
    boolean cancelled;
    boolean enrolled;

    public static final String SELECT = """
            SELECT ts.id, ts.training_id, ts.slot_date, ts.cancelled,
                (SELECT COUNT(*) FROM training_enrollment te WHERE te.slot_id = ts.id) AS enrolled_count,
                (SELECT COUNT(*) FROM training_comment tc WHERE tc.slot_id = ts.id) AS comments_count,
                COALESCE(ts.start_time, t.start_time) AS start_time,
                COALESCE(ts.end_time, t.end_time) AS end_time,
                COALESCE(ts.max_players, t.max_players) AS max_players,
                COALESCE(ts.features, t.features) AS features,
                (ts.start_time IS NOT NULL OR ts.end_time IS NOT NULL OR ts.max_players IS NOT NULL OR ts.features IS NOT NULL) AS overridden,
                COALESCE(EXISTS(
                    SELECT 1 FROM training_enrollment tev
                    WHERE tev.slot_id = ts.id AND tev.user_id = :viewerId
                ), false) AS is_enrolled,
                t.day_of_week,
                c.id AS club_id, c.name AS club_name
            FROM training_slot ts
            JOIN training t ON t.id = ts.training_id
            JOIN club c ON c.id = t.club_id
            """;

    public static final RowMapper<TrainingSlot> ROW_MAPPER = (rs, rowNum) -> TrainingSlot.builder()
        .id(UUID.fromString(rs.getString("id")))
        .trainingId(UUID.fromString(rs.getString("training_id")))
        .clubId(UUID.fromString(rs.getString("club_id")))
        .clubName(rs.getString("club_name"))
        .slotDate(rs.getDate("slot_date").toLocalDate())
        .dayOfWeek(DayOfWeek.of(rs.getInt("day_of_week")))
        .startTime(rs.getTime("start_time").toLocalTime())
        .endTime(rs.getTime("end_time").toLocalTime())
        .enrolledCount(rs.getInt("enrolled_count"))
        .maxPlayers(rs.getInt("max_players"))
        .commentsCount(rs.getInt("comments_count"))
        .features(rs.getString("features"))
        .overridden(rs.getBoolean("overridden"))
        .cancelled(rs.getBoolean("cancelled"))
        .enrolled(rs.getBoolean("is_enrolled"))
    .build();
}