package com.github.bogdanovmn.comeplay.club;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void updateKeepsNullDescription() {
        UUID clubId = UUID.randomUUID();

        repository.update(clubId, "Клуб", 1, null, 7);

        ArgumentCaptor<SqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(anyString(), paramsCaptor.capture());
        SqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("description")).isNull();
        assertThat(params.getValue("scheduleDays")).isEqualTo(7);
    }

    @Test
    void createPersistsScheduleDays() {
        UUID ownerId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(UUID.class)))
            .thenReturn(UUID.randomUUID());

        repository.create("Клуб", 1, null, 14, ownerId);

        ArgumentCaptor<SqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForObject(anyString(), paramsCaptor.capture(), eq(UUID.class));
        assertThat(paramsCaptor.getValue().getValue("scheduleDays")).isEqualTo(14);
    }

    @Test
    void findByIdReadsScheduleDays() {
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());

        repository.findById(UUID.randomUUID());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), anyMap(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("c.schedule_days");
    }

    @Test
    void listCommentsTakesNewestCommentsAndKeepsNewestFirst() {
        UUID clubId = UUID.randomUUID();
        UUID olderId = UUID.randomUUID();
        UUID newerId = UUID.randomUUID();
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of(
            comment(newerId, "новый"),
            comment(olderId, "старый")
        ));

        List<ClubComment> comments = repository.listComments(clubId, 100);

        assertThat(comments).extracting(ClubComment::getId).containsExactly(newerId, olderId);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), anyMap(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
            .contains("ORDER BY cc.created_at DESC, cc.id DESC")
            .contains("LIMIT :limit");
    }

    private static ClubComment comment(UUID id, String text) {
        return ClubComment.builder().id(id).clubId(UUID.randomUUID()).text(text).build();
    }
}