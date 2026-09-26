package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.common.TrainingSlot;
import com.github.bogdanovmn.comeplay.security.AccessManagement;
import com.github.bogdanovmn.comeplay.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private final UUID slotId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Training training(DayOfWeek dayOfWeek) {
        return training(dayOfWeek, false);
    }

    private Training training(DayOfWeek dayOfWeek, boolean oneOff) {
        return Training.builder()
            .id(trainingId)
            .clubId(UUID.randomUUID())
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(18, 0))
            .endTime(LocalTime.of(20, 0))
            .maxPlayers(10)
            .features(null)
            .oneOff(oneOff)
            .build();
    }

    private TrainingSlot slot(boolean cancelled) {
        return slot(cancelled, 0, 10);
    }

    private TrainingSlot slot(boolean cancelled, int enrolledCount, int maxPlayers) {
        return TrainingSlot.builder()
            .id(slotId)
            .trainingId(trainingId)
            .clubId(UUID.randomUUID())
            .clubName("Клуб")
            .slotDate(LocalDate.now())
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(18, 0))
            .endTime(LocalTime.of(20, 0))
            .enrolledCount(enrolledCount)
            .waitlistCount(0)
            .maxPlayers(maxPlayers)
            .commentsCount(0)
            .features(null)
            .overridden(false)
            .cancelled(cancelled)
            .enrolled(false)
            .build();
    }

    private CreateTrainingRequest request(DayOfWeek dayOfWeek) {
        return request(dayOfWeek, false);
    }

    private CreateTrainingRequest request(DayOfWeek dayOfWeek, boolean oneOff) {
        return CreateTrainingRequest.builder()
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(18, 0))
            .endTime(LocalTime.of(20, 0))
            .maxPlayers(10)
            .features(null)
            .oneOff(oneOff)
            .build();
    }

    @Test
    void updateDeletesFutureSlotsWhenDayOfWeekChanged() {
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.update(trainingId, request(DayOfWeek.WEDNESDAY), userId);

        verify(trainingRepository).deleteFutureSlots(trainingId);
    }

    @Test
    void createDoesNotCreateSlotForRecurringTraining() {
        when(trainingRepository.create(any(), anyInt(), any(), any(), anyInt(), any(), anyBoolean())).thenReturn(trainingId);

        trainingService.create(UUID.randomUUID(), request(DayOfWeek.MONDAY), userId);

        verify(trainingRepository, never()).createSlotIfNotExists(any(), any());
    }

    @Test
    void createCreatesSingleSlotOnNearestDateForOneOffTraining() {
        UUID clubId = UUID.randomUUID();
        when(trainingRepository.create(any(), anyInt(), any(), any(), anyInt(), any(), anyBoolean())).thenReturn(trainingId);
        LocalDate expectedDate = TrainingService.nearestSlotDate(
            DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalDate.now(), LocalTime.now()
        );

        TrainingBrief created = trainingService.create(clubId, request(DayOfWeek.MONDAY, true), userId);

        verify(trainingRepository).createSlotIfNotExists(trainingId, expectedDate);
        assertTrue(created.isOneOff());
        assertEquals(expectedDate, created.getSlotDate());
    }

    @Test
    void updateMovesOneOffSlotWhenDayOfWeekChanged() {
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY, true)));
        when(trainingRepository.findNextSlotDate(trainingId)).thenReturn(Optional.of(LocalDate.now().plusDays(7)));

        trainingService.update(trainingId, request(DayOfWeek.WEDNESDAY, true), userId);

        verify(trainingRepository, never()).deleteFutureSlots(any());
        verify(trainingRepository).moveSlot(
            eq(trainingId),
            eq(LocalDate.now().plusDays(7)),
            any(LocalDate.class)
        );
    }

    @Test
    void updateKeepsOneOffSlotWhenDayOfWeekUnchanged() {
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY, true)));

        trainingService.update(trainingId, request(DayOfWeek.MONDAY, true), userId);

        verify(trainingRepository, never()).moveSlot(any(), any(), any());
        verify(trainingRepository, never()).deleteFutureSlots(any());
    }

    @Test
    void updateCreatesOneOffSlotWhenOnlyPastSlotExists() {
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY, true)));
        when(trainingRepository.findNextSlotDate(trainingId)).thenReturn(Optional.empty());

        trainingService.update(trainingId, request(DayOfWeek.WEDNESDAY, true), userId);

        verify(trainingRepository, never()).moveSlot(any(), any(), any());
        verify(trainingRepository).createSlotIfNotExists(eq(trainingId), any(LocalDate.class));
    }

    @Test
    void nearestSlotDateKeepsTodayWhenStartTimeIsNotPassedYet() {
        LocalDate monday = LocalDate.of(2026, 9, 28);

        LocalDate result = TrainingService.nearestSlotDate(DayOfWeek.MONDAY, LocalTime.of(18, 0), monday, LocalTime.of(10, 0));

        assertEquals(monday, result);
    }

    @Test
    void nearestSlotDateMovesToNextWeekWhenStartTimeIsAlreadyPassed() {
        LocalDate monday = LocalDate.of(2026, 9, 28);

        LocalDate result = TrainingService.nearestSlotDate(DayOfWeek.MONDAY, LocalTime.of(18, 0), monday, LocalTime.of(19, 0));

        assertEquals(LocalDate.of(2026, 10, 5), result);
    }

    @Test
    void nearestSlotDateFindsClosestDateWithGivenDayOfWeek() {
        LocalDate monday = LocalDate.of(2026, 9, 28);

        assertEquals(
            LocalDate.of(2026, 9, 30),
            TrainingService.nearestSlotDate(DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), monday, LocalTime.of(19, 0))
        );
        assertEquals(
            LocalDate.of(2026, 10, 4),
            TrainingService.nearestSlotDate(DayOfWeek.SUNDAY, LocalTime.of(18, 0), monday, LocalTime.of(19, 0))
        );
    }

    @Test
    void updateKeepsFutureSlotsWhenDayOfWeekUnchanged() {
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.update(trainingId, request(DayOfWeek.MONDAY), userId);

        verify(trainingRepository, never()).deleteFutureSlots(trainingId);
    }

    @Test
    void enrollIsRejectedOnCancelledSlot() {
        when(trainingRepository.findSlotById(eq(slotId), any())).thenReturn(Optional.of(slot(true)));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        assertThrows(IllegalArgumentException.class, () -> trainingService.enroll(slotId, userId, userId));

        verify(trainingRepository, never()).enroll(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void enrollGoesToWaitlistWhenSlotIsFull() {
        when(trainingRepository.findSlotById(eq(slotId), any())).thenReturn(Optional.of(slot(false, 10, 10)));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.enroll(slotId, userId, userId);

        verify(trainingRepository).enroll(slotId, userId, null, userId, true);
    }

    @Test
    void enrollIsNotWaitlistWhenSlotHasFreePlaces() {
        when(trainingRepository.findSlotById(eq(slotId), any())).thenReturn(Optional.of(slot(false, 8, 10)));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.enroll(slotId, userId, userId);

        verify(trainingRepository).enroll(slotId, userId, null, userId, false);
    }

    @Test
    void unenrollPromotesFromWaitlist() {
        trainingService.unenroll(slotId, userId);

        verify(trainingRepository).unenroll(slotId, userId, null);
        verify(trainingRepository).promoteFromWaitlist(slotId);
    }

    @Test
    void updateSlotPromotesFromWaitlistWhenMaxPlayersIncreased() {
        when(trainingRepository.findSlotById(eq(slotId), any())).thenReturn(Optional.of(slot(false, 10, 10)));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.updateSlot(slotId, new UpdateSlotRequest(
            LocalTime.of(18, 0), LocalTime.of(20, 0), 20, null
        ), userId);

        verify(trainingRepository).promoteFromWaitlist(slotId);
    }

    @Test
    void cancelSlotSetsCancelledForOwner() {
        when(trainingRepository.findSlotById(eq(slotId), any())).thenReturn(Optional.of(slot(false)));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.cancelSlot(slotId, userId);

        verify(trainingRepository).setSlotCancelled(slotId, true);
    }

    @Test
    void restoreSlotRemovesCancelledFlagForOwner() {
        when(trainingRepository.findSlotById(eq(slotId), any())).thenReturn(Optional.of(slot(true)));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));

        trainingService.restoreSlot(slotId, userId);

        verify(trainingRepository).setSlotCancelled(slotId, false);
    }

    @Test
    void cancelSlotRejectedForNonOwner() {
        when(trainingRepository.findSlotById(eq(slotId), any())).thenReturn(Optional.of(slot(false)));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training(DayOfWeek.MONDAY)));
        doThrow(new AccessDeniedException("no")).when(accessManagement).requireOwner(any(), any());

        assertThrows(AccessDeniedException.class, () -> trainingService.cancelSlot(slotId, userId));

        verify(trainingRepository, never()).setSlotCancelled(any(), anyBoolean());
    }
}