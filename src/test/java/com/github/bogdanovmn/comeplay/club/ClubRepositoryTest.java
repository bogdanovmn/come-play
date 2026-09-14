package com.github.bogdanovmn.comeplay.club;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClubRepositoryTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final ClubRepository repository = new ClubRepository(jdbc);

    @Test
    void listInvitationsCountsJoinersPerInvitation() {
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());

        repository.listInvitations(UUID.randomUUID());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), anyMap(), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertThat(sql)
                .contains("FROM invitation_history")
                .contains("WHERE ji.invitation_id = i.id")
                .doesNotContain("club_member");
    }
}