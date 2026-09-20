package com.github.bogdanovmn.comeplay.training;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}