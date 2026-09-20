package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.security.AccessManagement;
import com.github.bogdanovmn.comeplay.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TrainingService {

    private final TrainingRepository trainingRepository;
    private final UserRepository userRepository;
    private final AccessManagement accessManagement;

    @Transactional(readOnly = true)
    public List<TrainingBrief> listByClub(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return trainingRepository.listByClub(clubId);
    }

    @Transactional(readOnly = true)
    public TrainingSlot getSlot(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        requireClubOpenForMember(slot.getClubId(), userId);
        return slot;
    }

    @Transactional
    public TrainingBrief create(UUID clubId, CreateTrainingRequest request, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        requireValidTimeRange(request.getStartTime(), request.getEndTime());
        UUID trainingId = trainingRepository.create(
            clubId,
            request.getDayOfWeek().getValue(),
            request.getStartTime(),
            request.getEndTime(),
            request.getMaxPlayers(),
            request.getFeatures()
        );
        return TrainingBrief.builder()
            .id(trainingId)
            .dayOfWeek(request.getDayOfWeek())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .maxPlayers(request.getMaxPlayers())
            .features(request.getFeatures())
            .build();
    }

    @Transactional
    public TrainingBrief update(UUID trainingId, CreateTrainingRequest request, UUID userId) {
        Training training = trainingRepository.findById(trainingId)
            .orElseThrow(() -> new NoSuchElementException("Training not found: " + trainingId));
        accessManagement.requireOwner(training.getClubId(), userId);
        requireValidTimeRange(request.getStartTime(), request.getEndTime());
        boolean dayChanged = training.getDayOfWeek() != request.getDayOfWeek();
        trainingRepository.update(
            trainingId,
            request.getDayOfWeek().getValue(),
            request.getStartTime(),
            request.getEndTime(),
            request.getMaxPlayers(),
            request.getFeatures()
        );
        if (dayChanged) {
            trainingRepository.deleteFutureSlots(trainingId);
        }
        return TrainingBrief.builder()
            .id(trainingId)
            .dayOfWeek(request.getDayOfWeek())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .maxPlayers(request.getMaxPlayers())
            .features(request.getFeatures())
            .build();
    }

    @Transactional
    public void delete(UUID trainingId, UUID userId) {
        Training training = trainingRepository.findById(trainingId)
            .orElseThrow(() -> new NoSuchElementException("Training not found: " + trainingId));
        accessManagement.requireOwner(training.getClubId(), userId);
        trainingRepository.delete(trainingId);
    }

    @Transactional
    public List<TrainingSlot> listSlots(UUID clubId, LocalDate from, LocalDate to, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        requireClubOpenForMember(clubId, userId);
        if (!trainingRepository.clubClosed(clubId)) {
            trainingRepository.ensureSlots(clubId, from, to);
        }
        return trainingRepository.listSlots(clubId, from, to);
    }

    @Transactional(readOnly = true)
    public List<TrainingSlot> listSlotsByTraining(UUID trainingId, UUID userId) {
        Training training = trainingRepository.findById(trainingId)
            .orElseThrow(() -> new NoSuchElementException("Training not found: " + trainingId));
        requireClubOpenForMember(training.getClubId(), userId);
        return trainingRepository.listSlotsByTraining(trainingId);
    }

    @Transactional
    public void enroll(UUID slotId, UUID userId, UUID enrolledBy) {
        TrainingSlot slot = requireSlotAvailable(slotId);
        Training training = requireTraining(slot.getTrainingId());
        requireClubActive(training.getClubId());
        accessManagement.requireMember(training.getClubId(), userId);
        accessManagement.requireMember(training.getClubId(), enrolledBy);

        trainingRepository.enroll(slotId, userId, null, enrolledBy);
    }

    @Transactional
    public void enrollFriend(UUID slotId, UUID friendId, UUID enrolledBy) {
        TrainingSlot slot = requireSlotAvailable(slotId);
        requireFriend(friendId, enrolledBy);
        Training training = requireTraining(slot.getTrainingId());
        requireClubActive(training.getClubId());
        accessManagement.requireMember(training.getClubId(), enrolledBy);

        trainingRepository.enroll(slotId, null, friendId, enrolledBy);
    }

    @Transactional
    public void unenroll(UUID slotId, UUID userId) {
        trainingRepository.unenroll(slotId, userId, null);
    }

    @Transactional
    public void unenrollFriend(UUID slotId, UUID friendId, UUID userId) {
        requireFriend(friendId, userId);
        trainingRepository.unenroll(slotId, null, friendId);
    }

    @Transactional
    public void setComingLater(UUID slotId, boolean comingLater, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        Training training = requireTraining(slot.getTrainingId());
        accessManagement.requireMember(training.getClubId(), userId);
        trainingRepository.setComingLater(slotId, userId, comingLater);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> listEnrollments(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        requireClubOpenForMember(slot.getClubId(), userId);
        return trainingRepository.listEnrollments(slotId);
    }

    @Transactional(readOnly = true)
    public List<Comment> listComments(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        requireClubOpenForMember(slot.getClubId(), userId);
        return trainingRepository.listComments(slotId);
    }

    @Transactional
    public Comment createComment(UUID slotId, String text, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        requireClubOpenForMember(slot.getClubId(), userId);
        UUID commentId = trainingRepository.createComment(slotId, userId, text);
        String authorName = trainingRepository.findUserName(userId);
        return Comment.builder()
            .id(commentId)
            .slotId(slotId)
            .userId(userId)
            .authorName(authorName)
            .text(text)
        .build();
    }

    @Transactional
    public TrainingSlot updateSlot(UUID slotId, UpdateSlotRequest request, UUID userId) {
        requireValidTimeRange(request.getStartTime(), request.getEndTime());
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        Training training = requireTraining(slot.getTrainingId());
        accessManagement.requireOwner(training.getClubId(), userId);
        if (request.getMaxPlayers() < slot.getEnrolledCount()) {
            throw new IllegalArgumentException(
                "Max players %d cannot be less than enrolled players %d".formatted(request.getMaxPlayers(), slot.getEnrolledCount())
            );
        }
        trainingRepository.updateSlotParams(
            slotId,
            request.getStartTime(),
            request.getEndTime(),
            request.getMaxPlayers(),
            request.getFeatures()
        );
        return trainingRepository.findSlotById(slotId).orElseThrow();
    }

    @Transactional
    public TrainingSlot clearSlotOverrides(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        Training training = requireTraining(slot.getTrainingId());
        accessManagement.requireOwner(training.getClubId(), userId);
        trainingRepository.clearSlotParams(slotId);
        return trainingRepository.findSlotById(slotId).orElseThrow();
    }

    private void requireValidTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Начало тренировки должно быть раньше конца");
        }
    }

    private void requireClubOpenForMember(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        if (trainingRepository.clubClosed(clubId) && !accessManagement.isOwner(clubId, userId)) {
            throw new AccessDeniedException("Club is closed: " + clubId);
        }
    }

    private void requireClubActive(UUID clubId) {
        if (trainingRepository.clubClosed(clubId)) {
            throw new AccessDeniedException("Club is closed: " + clubId);
        }
    }

    private TrainingSlot requireSlotAvailable(UUID slotId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        if (slot.getEnrolledCount() >= slot.getMaxPlayers()) {
            throw new IllegalArgumentException("Slot is full: " + slotId);
        }
        return slot;
    }

    private Training requireTraining(UUID trainingId) {
        return trainingRepository.findById(trainingId)
            .orElseThrow(() -> new NoSuchElementException("Training not found: " + trainingId));
    }

    private void requireFriend(UUID friendId, UUID userId) {
        if (friendId == null) {
            throw new IllegalArgumentException("friendId is required");
        }
        userRepository.findFriend(friendId, userId).orElseThrow(
            () -> new IllegalArgumentException("Friend not found: " + friendId)
        );
    }
}