package com.github.bogdanovmn.comeplay.sport;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class SportTypeRepository {

    private final NamedParameterJdbcTemplate jdbc;

    List<SportType> findAll() {
        return jdbc.query("""
                SELECT id, name FROM sport_type ORDER BY name
                """,
                (rs, rowNum) -> SportType.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .build()
        );
    }

    Optional<SportType> findById(int id) {
        var result = jdbc.query("""
                SELECT id, name FROM sport_type WHERE id = :id
                """,
                Map.of("id", id),
                (rs, rowNum) -> SportType.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .build()
        );
        return result.stream().findFirst();
    }

    int insert(String name) {
        return jdbc.queryForObject("""
                INSERT INTO sport_type (name) VALUES (:name)
                RETURNING id
                """,
                Map.of("name", name),
                Integer.class
        );
    }
}