package com.github.bogdanovmn.comeplay.training;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingRepositoryTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final TrainingRepository repository = new TrainingRepository(jdbc);

    @Test
    void deleteFutureSlotsRemovesCommentsEnrollmentsAndSlotsForUpcomingDates() {
        UUID trainingId = UUID.randomUUID();

        repository.deleteFutureSlots(trainingId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(3)).update(sqlCaptor.capture(), anyMap());
        List<String> sqls = sqlCaptor.getAllValues();
        assertThat(sqls).hasSize(3);
        assertThat(sqls.get(0))
            .contains("DELETE FROM training_comment")
            .contains("slot_id IN",
                "training_id = :trainingId",
                "slot_date >= CURRENT_DATE");
        assertThat(sqls.get(1))
            .contains("DELETE FROM training_enrollment")
            .contains("slot_id IN",
                "training_id = :trainingId",
                "slot_date >= CURRENT_DATE");
        assertThat(sqls.get(2))
            .contains("DELETE FROM training_slot")
            .contains("training_id = :trainingId",
                "slot_date >= CURRENT_DATE");
    }

    @Test
    void ensureSlotsSkipsOneOffTrainings() {
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());

        repository.ensureSlots(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sqlCaptor.capture(), anyMap());
        assertThat(sqlCaptor.getValue())
            .contains("FROM training")
            .contains("club_id = :clubId")
            .contains("one_off = false");
    }

    @Test
    void listByClubReturnsOneOffFlagAndSlotDate() {
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());

        repository.listByClub(UUID.randomUUID());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), anyMap(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
            .contains("t.one_off")
            .contains("CASE WHEN t.one_off")
            .contains("FROM training_slot ts WHERE ts.training_id = t.id");
    }
}