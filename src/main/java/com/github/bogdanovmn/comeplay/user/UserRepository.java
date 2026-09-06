package com.github.bogdanovmn.comeplay.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class UserRepository {

    private static final RowMapper<UserProfile> PROFILE_ROW_MAPPER = (rs, rowNum) -> UserProfile.builder()
            .id(UUID.fromString(rs.getString("id")))
            .displayName(rs.getString("display_name"))
            .build();

    private static final RowMapper<FriendBrief> FRIEND_BRIEF_ROW_MAPPER = (rs, rowNum) -> FriendBrief.builder()
            .id(UUID.fromString(rs.getString("id")))
            .displayName(rs.getString("display_name"))
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

    UserProfile getOrCreate(UUID userId) {
        jdbc.update("""
                INSERT INTO app_user (id, display_name) VALUES (:userId, :displayName)
                ON CONFLICT (id) DO NOTHING
                """,
                Map.of("userId", userId, "displayName", "Player")
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
                SELECT u.id, u.display_name
                FROM friendship f
                JOIN app_user u ON u.id = f.friend_id
                WHERE f.user_id = :userId
                ORDER BY u.display_name
                """,
                Map.of("userId", userId),
                FRIEND_BRIEF_ROW_MAPPER
        );
    }

    void addFriend(UUID userId, UUID friendId) {
        jdbc.update("""
                INSERT INTO friendship (user_id, friend_id) VALUES (:userId, :friendId)
                ON CONFLICT DO NOTHING
                """,
                Map.of("userId", userId, "friendId", friendId)
        );
    }

    void removeFriend(UUID userId, UUID friendId) {
        jdbc.update("""
                DELETE FROM friendship WHERE user_id = :userId AND friend_id = :friendId
                """,
                Map.of("userId", userId, "friendId", friendId)
        );
    }

    List<UserProfile> searchByDisplayName(String term, UUID excludeUserId) {
        return jdbc.query("""
                SELECT id, display_name FROM app_user
                WHERE LOWER(display_name) LIKE LOWER(:term) AND id != :excludeUserId
                LIMIT 20
                """,
                Map.of("term", "%" + term + "%", "excludeUserId", excludeUserId),
                PROFILE_ROW_MAPPER
        );
    }
}
