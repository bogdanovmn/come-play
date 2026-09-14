package com.github.bogdanovmn.comeplay.user;

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
public class UserRepository {

    private static final RowMapper<UserProfile> PROFILE_ROW_MAPPER = (rs, rowNum) -> UserProfile.builder()
            .id(UUID.fromString(rs.getString("id")))
            .displayName(rs.getString("display_name"))
            .build();

    private static final RowMapper<FriendBrief> FRIEND_BRIEF_ROW_MAPPER = (rs, rowNum) -> FriendBrief.builder()
            .id(UUID.fromString(rs.getString("id")))
            .name(rs.getString("name"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;

    Optional<UserProfile> findById(UUID userId) {
        var result = jdbc.query("""
                SELECT id, display_name FROM app_user WHERE id = :userId
                """,
                Map.of("userId", userId),
                PROFILE_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    UserProfile getOrCreate(UUID userId, String displayName) {
        jdbc.update("""
                INSERT INTO app_user (id, display_name) VALUES (:userId, :displayName)
                ON CONFLICT (id) DO NOTHING
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("displayName", displayName == null ? "Player" : displayName)
        );
        return findById(userId).orElseThrow();
    }

    void updateDisplayName(UUID userId, String displayName) {
        jdbc.update("""
                UPDATE app_user SET display_name = :displayName WHERE id = :userId
                """,
                Map.of("userId", userId, "displayName", displayName)
        );
    }

    List<FriendBrief> listFriends(UUID userId) {
        return jdbc.query("""
                SELECT id, name
                FROM friend
                WHERE user_id = :userId
                ORDER BY name
                """,
                Map.of("userId", userId),
                FRIEND_BRIEF_ROW_MAPPER
        );
    }

    FriendBrief createFriend(UUID userId, String name) {
        var existing = jdbc.query("""
                SELECT id, name FROM friend WHERE user_id = :userId AND name = :name
                """,
                Map.of("userId", userId, "name", name),
                FRIEND_BRIEF_ROW_MAPPER
        );
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Friend with name already exists: " + name);
        }
        UUID id = jdbc.queryForObject("""
                INSERT INTO friend (user_id, name) VALUES (:userId, :name)
                RETURNING id
                """,
                Map.of("userId", userId, "name", name),
                UUID.class
        );
        return FriendBrief.builder().id(id).name(name).build();
    }

    void deleteFriend(UUID userId, UUID friendId) {
        jdbc.update("""
                DELETE FROM friend WHERE id = :friendId AND user_id = :userId
                """,
                Map.of("userId", userId, "friendId", friendId)
        );
    }

    public Optional<FriendBrief> findFriend(UUID friendId, UUID userId) {
        var result = jdbc.query("""
                SELECT id, name FROM friend
                WHERE id = :friendId AND user_id = :userId
                """,
                Map.of("friendId", friendId, "userId", userId),
                FRIEND_BRIEF_ROW_MAPPER
        );
        return result.stream().findFirst();
    }
}
