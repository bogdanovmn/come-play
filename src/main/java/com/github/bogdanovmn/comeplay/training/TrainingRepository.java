package com.github.bogdanovmn.comeplay.training;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.DayOfWeek;
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
        .dayOfWeek(DayOfWeek.of(rs.getInt("day_of_week")))
        .startTime(rs.getTime("start_time").toLocalTime())
        .endTime(rs.getTime("end_time").toLocalTime())
        .maxPlayers(rs.getInt("max_players"))
        .features(rs.getString("features"))
    .build();

    private static final RowMapper<TrainingSlot> SLOT_ROW_MAPPER = (rs, rowNum) -> TrainingSlot.builder()
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
        .build();

    private static final String SLOT_SELECT = """
            SELECT ts.id, ts.training_id, ts.slot_date,
                (SELECT COUNT(*) FROM training_enrollment te WHERE te.slot_id = ts.id) AS enrolled_count,
                (SELECT COUNT(*) FROM training_comment tc WHERE tc.slot_id = ts.id) AS comments_count,
                t.day_of_week,
                COALESCE(ts.start_time, t.start_time) AS start_time,
                COALESCE(ts.end_time, t.end_time) AS end_time,
                COALESCE(ts.max_players, t.max_players) AS max_players,
                COALESCE(ts.features, t.features) AS features,
                (ts.start_time IS NOT NULL OR ts.end_time IS NOT NULL OR ts.max_players IS NOT NULL OR ts.features IS NOT NULL) AS overridden,
                c.id AS club_id, c.name AS club_name
            FROM training_slot ts
            JOIN training t ON t.id = ts.training_id
            JOIN club c ON c.id = t.club_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    List<TrainingBrief> listByClub(UUID clubId) {
        return jdbc.query("""
                SELECT id, day_of_week, start_time, end_time, max_players, features
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
                SELECT id, club_id, day_of_week, start_time, end_time, max_players, features
                FROM training
                WHERE id = :trainingId
                """,
            Map.of("trainingId", trainingId),
            (rs, rowNum) -> Training.builder()
                .id(UUID.fromString(rs.getString("id")))
                .clubId(UUID.fromString(rs.getString("club_id")))
                .dayOfWeek(DayOfWeek.of(rs.getInt("day_of_week")))
                .startTime(rs.getTime("start_time").toLocalTime())
                .endTime(rs.getTime("end_time").toLocalTime())
                .maxPlayers(rs.getInt("max_players"))
                .features(rs.getString("features"))
            .build()
        );
        return result.stream().findFirst();
    }

    UUID create(UUID clubId, int dayOfWeek, LocalTime startTime, LocalTime endTime, int maxPlayers, String features) {
        return jdbc.queryForObject("""
                INSERT INTO training (club_id, day_of_week, start_time, end_time, max_players, features)
                VALUES (:clubId, :dayOfWeek, :startTime, :endTime, :maxPlayers, :features)
                RETURNING id
                """,
            new MapSqlParameterSource()
                .addValue("clubId", clubId)
                .addValue("dayOfWeek", dayOfWeek)
                .addValue("startTime", Time.valueOf(startTime))
                .addValue("endTime", Time.valueOf(endTime))
                .addValue("maxPlayers", maxPlayers)
                .addValue("features", features),
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

    void deleteFutureSlots(UUID trainingId) {
        jdbc.update("""
                DELETE FROM training_comment
                WHERE slot_id IN (
                    SELECT id FROM training_slot
                    WHERE training_id = :trainingId AND slot_date >= CURRENT_DATE
                )
                """,
            Map.of("trainingId", trainingId)
        );
        jdbc.update("""
                DELETE FROM training_enrollment
                WHERE slot_id IN (
                    SELECT id FROM training_slot
                    WHERE training_id = :trainingId AND slot_date >= CURRENT_DATE
                )
                """,
            Map.of("trainingId", trainingId)
        );
        jdbc.update("""
                DELETE FROM training_slot
                WHERE training_id = :trainingId AND slot_date >= CURRENT_DATE
                """,
            Map.of("trainingId", trainingId)
        );
    }

    void update(UUID trainingId, int dayOfWeek, LocalTime startTime, LocalTime endTime, int maxPlayers, String features) {
        jdbc.update("""
                UPDATE training
                SET day_of_week = :dayOfWeek,
                    start_time = :startTime,
                    end_time = :endTime,
                    max_players = :maxPlayers,
                    features = :features
                WHERE id = :trainingId
                """,
            new MapSqlParameterSource()
                .addValue("trainingId", trainingId)
                .addValue("dayOfWeek", dayOfWeek)
                .addValue("startTime", Time.valueOf(startTime))
                .addValue("endTime", Time.valueOf(endTime))
                .addValue("maxPlayers", maxPlayers)
                .addValue("features", features)
        );
    }

    List<TrainingSlot> listSlots(UUID clubId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                %s
                WHERE t.club_id = :clubId
                    AND ts.slot_date >= :from AND ts.slot_date <= :to
                ORDER BY ts.slot_date, COALESCE(ts.start_time, t.start_time)
                """.formatted(SLOT_SELECT),
            Map.of("clubId", clubId, "from", from, "to", to),
            SLOT_ROW_MAPPER
        );
    }

    List<TrainingSlot> listSlotsByTraining(UUID trainingId) {
        return jdbc.query("""
                %s
                WHERE ts.training_id = :trainingId
                ORDER BY ts.slot_date
                """.formatted(SLOT_SELECT),
            Map.of("trainingId", trainingId),
            SLOT_ROW_MAPPER
        );
    }

    Optional<TrainingSlot> findSlotById(UUID slotId) {
        var result = jdbc.query("""
                %s
                WHERE ts.id = :slotId
                """.formatted(SLOT_SELECT),
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
            new MapSqlParameterSource()
                .addValue("slotId", slotId)
                .addValue("userId", userId)
                .addValue("friendId", friendId)
                .addValue("enrolledBy", enrolledBy)
        );
    }

    void unenroll(UUID slotId, UUID userId, UUID friendId) {
        String sql = friendId != null
            ? "DELETE FROM training_enrollment WHERE slot_id = :slotId AND friend_id = :friendId"
            : "DELETE FROM training_enrollment WHERE slot_id = :slotId AND user_id = :userId";
        jdbc.update(
            sql,
            new MapSqlParameterSource()
                .addValue("slotId", slotId)
                .addValue("userId", userId)
                .addValue("friendId", friendId)
        );
    }

    List<Enrollment> listEnrollments(UUID slotId) {
        return jdbc.query("""
                SELECT e.slot_id, e.user_id, e.friend_id,
                    COALESCE(u.display_name, f.name) AS name,
                    e.enrolled_by, e.enrolled_at, e.coming_later
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
                .comingLater(rs.getBoolean("coming_later"))
                .build()
        );
    }

    List<Comment> listComments(UUID slotId) {
        return jdbc.query("""
                SELECT c.id, c.slot_id, c.user_id, COALESCE(u.display_name, 'Игрок') AS author_name,
                    c.text, c.created_at
                FROM training_comment c
                LEFT JOIN app_user u ON u.id = c.user_id
                WHERE c.slot_id = :slotId
                ORDER BY c.created_at
                """,
            Map.of("slotId", slotId),
            (rs, rowNum) -> Comment.builder()
                .id(UUID.fromString(rs.getString("id")))
                .slotId(UUID.fromString(rs.getString("slot_id")))
                .userId(UUID.fromString(rs.getString("user_id")))
                .authorName(rs.getString("author_name"))
                .text(rs.getString("text"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
            .build()
        );
    }

    String findUserName(UUID userId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(display_name, 'Игрок')
                FROM app_user
                WHERE id = :userId
                """,
            Map.of("userId", userId),
            String.class
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

    boolean clubClosed(UUID clubId) {
        Boolean closed = jdbc.queryForObject(
            "SELECT closed FROM club WHERE id = :clubId",
            Map.of("clubId", clubId),
            Boolean.class
        );
        return closed != null && closed;
    }

    void updateSlotParams(UUID slotId, LocalTime startTime, LocalTime endTime, int maxPlayers, String features) {
        jdbc.update("""
                UPDATE training_slot
                SET start_time = :startTime,
                    end_time = :endTime,
                    max_players = :maxPlayers,
                    features = :features
                WHERE id = :slotId
                """,
            Map.of(
                "slotId", slotId,
                "startTime", Time.valueOf(startTime),
                "endTime", Time.valueOf(endTime),
                "maxPlayers", maxPlayers,
                "features", features
            )
        );
    }

    void clearSlotParams(UUID slotId) {
        jdbc.update("""
                UPDATE training_slot
                SET start_time = NULL,
                    end_time = NULL,
                    max_players = NULL,
                    features = NULL
                WHERE id = :slotId
                """,
            Map.of("slotId", slotId)
        );
    }

    void setComingLater(UUID slotId, UUID userId, boolean comingLater) {
        jdbc.update("""
                UPDATE training_enrollment
                SET coming_later = :comingLater
                WHERE slot_id = :slotId AND user_id = :userId AND friend_id IS NULL
                """,
            Map.of(
                "slotId", slotId,
                "userId", userId,
                "comingLater", comingLater
            )
        );
    }
}
