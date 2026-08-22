package com.github.bogdanovmn.comeplay.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccessManagementRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public boolean isOwner(UUID clubId, UUID userId) {
        var result = jdbc.queryForList("""
                SELECT 1 FROM club WHERE id = :clubId AND owner_id = :userId
                """,
                Map.of("clubId", clubId, "userId", userId)
        );
        return !result.isEmpty();
    }

    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public boolean isMember(UUID clubId, UUID userId) {
        var result = jdbc.queryForList("""
                SELECT 1 FROM club_member WHERE club_id = :clubId AND user_id = :userId
                """,
                Map.of("clubId", clubId, "userId", userId)
        );
        return !result.isEmpty();
    }
}
