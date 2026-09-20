package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.security.AccessManagement;
import com.github.bogdanovmn.comeplay.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingServiceTest {

    private final TrainingRepository trainingRepository = mock(TrainingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccessManagement accessManagement = mock(AccessManagement.class);
    private final TrainingService trainingService = new TrainingService(trainingRepository, userRepository, accessManagement);

    private final UUID trainingId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Training training(DayOfWeek dayOfWeek) {
        return Training.builder()
            .id(trainingId)
            .clubId(UUID.randomUUID())
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(18, 0))
            .endTime(LocalTime.of(20, 0))
            .maxPlayers(10)
            .features(null)
            .build();
    }

    private CreateTrainingRequest request(DayOfWeek dayOfWeek) {
        return CreateTrainingRequest.builder()
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(18, 0))
            .endTime(LocalTime.of(20, 0))
            .maxPlayers(10)
            .features(null)
            .build();
    }

    @Test
    void updateDeletesFutureSlotsWhenDayOfWeekChanged() {
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.update(trainingId, request(DayOfWeek.WEDNESDAY), userId);

        verify(trainingRepository).deleteFutureSlots(trainingId);
    }

    @Test
    void updateKeepsFutureSlotsWhenDayOfWeekUnchanged() {
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.update(trainingId, request(DayOfWeek.MONDAY), userId);

        verify(trainingRepository, never()).deleteFutureSlots(trainingId);
    }
}