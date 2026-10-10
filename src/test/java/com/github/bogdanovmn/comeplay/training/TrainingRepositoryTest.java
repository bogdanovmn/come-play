package com.github.bogdanovmn.comeplay.training;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
            .contains("active = true")
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
            .contains("FROM training_slot ts WHERE ts.training_id = t.id")
            .contains("t.active = true");
    }

    @Test
    void updateSlotParamsKeepsNullFeatures() {
        UUID slotId = UUID.randomUUID();

        repository.updateSlotParams(slotId, LocalTime.of(18, 0), LocalTime.of(20, 0), 10, null);

        ArgumentCaptor<SqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(anyString(), paramsCaptor.capture());
        SqlParameterSource params = paramsCaptor.getValue();
        assertThat(params.getValue("features")).isNull();
        assertThat(params.getValue("slotId")).isEqualTo(slotId);
        assertThat(params.getValue("maxPlayers")).isEqualTo(10);
    }

    @Test
    void deleteKeepsPastSlotsAndSoftDeletesTraining() {
        UUID trainingId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(2);

        repository.delete(trainingId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(4)).update(sqlCaptor.capture(), anyMap());
        assertThat(sqlCaptor.getAllValues().get(3).trim())
            .isEqualTo("UPDATE training SET active = false WHERE id = :trainingId");
    }

    @Test
    void deleteRemovesTrainingWhenNoSlotsLeft() {
        UUID trainingId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(0);

        repository.delete(trainingId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(4)).update(sqlCaptor.capture(), anyMap());
        assertThat(sqlCaptor.getAllValues().get(3).trim())
            .isEqualTo("DELETE FROM training WHERE id = :trainingId");
    }
}