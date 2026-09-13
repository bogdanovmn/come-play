package com.github.bogdanovmn.comeplay.training;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class TrainingRepository {

    private static final RowMapper<TrainingBrief> TRAINING_BRIEF_ROW_MAPPER = (rs, rowNum) -> TrainingBrief.builder()
        .id(UUID.fromString(rs.getString("id")))
        .dayOfWeek(java.time.DayOfWeek.of(rs.getInt("day_of_week")))
        .startTime(rs.getTime("start_time").toLocalTime())
        .endTime(rs.getTime("end_time").toLocalTime())
        .maxPlayers(rs.getInt("max_players"))
    .build();

    private static final RowMapper<TrainingSlot> SLOT_ROW_MAPPER = (rs, rowNum) -> TrainingSlot.builder()
        .id(UUID.fromString(rs.getString("id")))
        .trainingId(UUID.fromString(rs.getString("training_id")))
        .slotDate(rs.getDate("slot_date").toLocalDate())
        .dayOfWeek(java.time.DayOfWeek.of(rs.getInt("day_of_week")))
        .startTime(rs.getTime("start_time").toLocalTime())
        .endTime(rs.getTime("end_time").toLocalTime())
        .enrolledCount(rs.getInt("enrolled_count"))
        .maxPlayers(rs.getInt("max_players"))
        .build();

    private final NamedParameterJdbcTemplate jdbc;

    List<TrainingBrief> listByClub(UUID clubId) {
        return jdbc.query("""
                SELECT id, day_of_week, start_time, end_time, max_players
                FROM training
                WHERE club_id = :clubId
                ORDER BY day_of_week, start_time
                """,
            Map.of("clubId", clubId),
            TRAINING_BRIEF_ROW_MAPPER
        );
    }

    Optional<Training> findById(UUID trainingId) {
        List<Training> result = jdbc.query("""
                SELECT id, club_id, day_of_week, start_time, end_time, max_players
                FROM training
                WHERE id = :trainingId
                """,
            Map.of("trainingId", trainingId),
            (rs, rowNum) -> Training.builder()
                .id(UUID.fromString(rs.getString("id")))
                .clubId(UUID.fromString(rs.getString("club_id")))
                .dayOfWeek(java.time.DayOfWeek.of(rs.getInt("day_of_week")))
                .startTime(rs.getTime("start_time").toLocalTime())
                .endTime(rs.getTime("end_time").toLocalTime())
                .maxPlayers(rs.getInt("max_players"))
            .build()
        );
        return result.stream().findFirst();
    }

    UUID create(UUID clubId, int dayOfWeek, LocalTime startTime, LocalTime endTime, int maxPlayers) {
        return jdbc.queryForObject("""
                INSERT INTO training (club_id, day_of_week, start_time, end_time, max_players)
                VALUES (:clubId, :dayOfWeek, :startTime, :endTime, :maxPlayers)
                RETURNING id
                """,
            Map.of(
                "clubId", clubId,
                "dayOfWeek", dayOfWeek,
                "startTime", Time.valueOf(startTime),
                "endTime", Time.valueOf(endTime),
                "maxPlayers", maxPlayers
            ),
            UUID.class
        );
    }

    void delete(UUID trainingId) {
        jdbc.update("""
                DELETE FROM training_comment
                WHERE slot_id IN (SELECT id FROM training_slot WHERE training_id = :trainingId)
                """,
            Map.of("trainingId", trainingId)
        );
        jdbc.update("""
                DELETE FROM training_enrollment
                WHERE slot_id IN (SELECT id FROM training_slot WHERE training_id = :trainingId)
                """,
            Map.of("trainingId", trainingId)
        );
        jdbc.update("""
                DELETE FROM training_slot
                WHERE training_id = :trainingId
                """,
            Map.of("trainingId", trainingId)
        );
        jdbc.update("""
                DELETE FROM training WHERE id = :trainingId
                """,
            Map.of("trainingId", trainingId)
        );
    }

    void update(UUID trainingId, int dayOfWeek, LocalTime startTime, LocalTime endTime, int maxPlayers) {
        jdbc.update("""
                UPDATE training
                SET day_of_week = :dayOfWeek,
                    start_time = :startTime,
                    end_time = :endTime,
                    max_players = :maxPlayers
                WHERE id = :trainingId
                """,
            Map.of(
                "trainingId", trainingId,
                "dayOfWeek", dayOfWeek,
                "startTime", Time.valueOf(startTime),
                "endTime", Time.valueOf(endTime),
                "maxPlayers", maxPlayers
            )
        );
    }

    List<TrainingSlot> listSlots(UUID clubId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT ts.id, ts.training_id, ts.slot_date,
                    (SELECT COUNT(*) FROM training_enrollment te WHERE te.slot_id = ts.id) AS enrolled_count,
                    t.day_of_week, t.start_time, t.end_time, t.max_players
                FROM training_slot ts
                JOIN training t ON t.id = ts.training_id
                WHERE t.club_id = :clubId
                    AND ts.slot_date >= :from AND ts.slot_date <= :to
                ORDER BY ts.slot_date, t.start_time
                """,
            Map.of("clubId", clubId, "from", from, "to", to),
            SLOT_ROW_MAPPER
        );
    }

    List<TrainingSlot> listSlotsByTraining(UUID trainingId) {
        return jdbc.query("""
                SELECT ts.id, ts.training_id, ts.slot_date,
                    (SELECT COUNT(*) FROM training_enrollment te WHERE te.slot_id = ts.id) AS enrolled_count,
                    t.day_of_week, t.start_time, t.end_time, t.max_players
                FROM training_slot ts
                JOIN training t ON t.id = ts.training_id
                WHERE ts.training_id = :trainingId
                ORDER BY ts.slot_date
                """,
            Map.of("trainingId", trainingId),
            SLOT_ROW_MAPPER
        );
    }

    Optional<TrainingSlot> findSlotById(UUID slotId) {
        var result = jdbc.query("""
                SELECT ts.id, ts.training_id, ts.slot_date,
                    (SELECT COUNT(*) FROM training_enrollment te WHERE te.slot_id = ts.id) AS enrolled_count,
                    t.day_of_week, t.start_time, t.end_time, t.max_players
                FROM training_slot ts
                JOIN training t ON t.id = ts.training_id
                WHERE ts.id = :slotId
                """,
            Map.of("slotId", slotId),
            SLOT_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    UUID createSlotIfNotExists(UUID trainingId, LocalDate slotDate) {
        return jdbc.queryForObject("""
                INSERT INTO training_slot (training_id, slot_date)
                VALUES (:trainingId, :slotDate)
                ON CONFLICT (training_id, slot_date)
                    DO UPDATE SET slot_date = EXCLUDED.slot_date
                RETURNING id
                """,
            Map.of("trainingId", trainingId, "slotDate", slotDate),
            UUID.class
        );
    }

    void ensureSlots(UUID clubId, LocalDate from, LocalDate to) {
        for (var template : jdbc.queryForList("""
                SELECT id, day_of_week
                FROM training
                WHERE club_id = :clubId
                """,
            Map.of("clubId", clubId)
        )) {
            UUID trainingId = (UUID) template.get("id");
            int dayOfWeek = ((Number) template.get("day_of_week")).intValue();
            LocalDate first = from;
            first = first.plusDays((dayOfWeek + 7 - first.getDayOfWeek().getValue()) % 7);
            for (LocalDate date = first; !date.isAfter(to); date = date.plusWeeks(1)) {
                createSlotIfNotExists(trainingId, date);
            }
        }
    }

    boolean isEnrolled(UUID slotId, UUID userId) {
        var result = jdbc.queryForList("""
                SELECT 1 FROM training_enrollment WHERE slot_id = :slotId AND user_id = :userId
                """,
            Map.of("slotId", slotId, "userId", userId)
        );
        return !result.isEmpty();
    }

    void enroll(UUID slotId, UUID userId, UUID friendId, UUID enrolledBy) {
        jdbc.update("""
                INSERT INTO training_enrollment (slot_id, user_id, friend_id, enrolled_by)
                VALUES (:slotId, :userId, :friendId, :enrolledBy)
                ON CONFLICT DO NOTHING
                """,
            Map.of(
                "slotId", slotId,
                "userId", userId,
                "friendId", friendId,
                "enrolledBy", enrolledBy
            )
        );
    }

    void unenroll(UUID slotId, UUID userId, UUID friendId) {
        String sql = friendId != null
            ? "DELETE FROM training_enrollment WHERE slot_id = :slotId AND friend_id = :friendId"
            : "DELETE FROM training_enrollment WHERE slot_id = :slotId AND user_id = :userId";
        jdbc.update(
            sql,
            Map.of("slotId", slotId, "userId", userId, "friendId", friendId)
        );
    }

    List<Enrollment> listEnrollments(UUID slotId) {
        return jdbc.query("""
                SELECT e.slot_id, e.user_id, e.friend_id,
                    COALESCE(u.display_name, f.name) AS name,
                    e.enrolled_by, e.enrolled_at
                FROM training_enrollment e
                LEFT JOIN app_user u ON u.id = e.user_id
                LEFT JOIN friend f ON f.id = e.friend_id
                WHERE e.slot_id = :slotId
                ORDER BY e.enrolled_at
                """,
            Map.of("slotId", slotId),
            (rs, rowNum) -> Enrollment.builder()
                .slotId(UUID.fromString(rs.getString("slot_id")))
                .userId(rs.getString("user_id") != null ? UUID.fromString(rs.getString("user_id")) : null)
                .friendId(rs.getString("friend_id") != null ? UUID.fromString(rs.getString("friend_id")) : null)
                .name(rs.getString("name"))
                .enrolledBy(UUID.fromString(rs.getString("enrolled_by")))
                .enrolledAt(rs.getTimestamp("enrolled_at").toInstant())
                .build()
        );
    }

    List<Comment> listComments(UUID slotId) {
        return jdbc.query("""
                SELECT id, slot_id, user_id, text, created_at
                FROM training_comment
                WHERE slot_id = :slotId
                ORDER BY created_at
                """,
            Map.of("slotId", slotId),
            (rs, rowNum) -> Comment.builder()
                .id(UUID.fromString(rs.getString("id")))
                .slotId(UUID.fromString(rs.getString("slot_id")))
                .userId(UUID.fromString(rs.getString("user_id")))
                .text(rs.getString("text"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
            .build()
        );
    }

    UUID createComment(UUID slotId, UUID userId, String text) {
        return jdbc.queryForObject("""
                INSERT INTO training_comment (slot_id, user_id, text)
                VALUES (:slotId, :userId, :text)
                RETURNING id
                """,
            Map.of(
                "slotId", slotId,
                "userId", userId,
                "text", text
            ),
            UUID.class
        );
    }
}
