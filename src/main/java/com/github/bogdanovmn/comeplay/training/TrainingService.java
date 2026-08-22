package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.club.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TrainingService {

    private final TrainingRepository trainingRepository;
    private final ClubService clubService;

    @Transactional(readOnly = true)
    public List<TrainingBrief> listByClub(UUID clubId, UUID userId) {
        requireMember(clubId, userId);
        return trainingRepository.listByClub(clubId);
    }

    @Transactional
    public TrainingBrief create(UUID clubId, CreateTrainingRequest request, UUID userId) {
        requireOwner(clubId, userId);
        UUID trainingId = trainingRepository.create(
                clubId,
                request.getSportType(),
                request.getDayOfWeek().getValue(),
                request.getStartTime().toString(),
                request.getEndTime().toString(),
                request.getMaxPlayers()
        );
        return TrainingBrief.builder()
                .id(trainingId)
                .sportType(request.getSportType())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxPlayers(request.getMaxPlayers())
                .build();
    }

    @Transactional
    public void delete(UUID trainingId, UUID userId) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Training not found: " + trainingId));
        requireOwner(training.getClubId(), userId);
        trainingRepository.delete(trainingId);
    }

    @Transactional(readOnly = true)
    public List<TrainingSlot> listSlots(UUID clubId, LocalDate from, LocalDate to, UUID userId) {
        requireMember(clubId, userId);
        return trainingRepository.listSlots(clubId, from, to);
    }

    @Transactional(readOnly = true)
    public List<TrainingSlot> listSlotsByTraining(UUID trainingId, UUID userId) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Training not found: " + trainingId));
        requireMember(training.getClubId(), userId);
        return trainingRepository.listSlotsByTraining(trainingId);
    }

    @Transactional
    public void enroll(UUID slotId, UUID userId, UUID enrolledBy) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Slot not found: " + slotId));

        if (slot.getEnrolledCount() >= slot.getMaxPlayers()) {
            throw new IllegalArgumentException("Slot is full: " + slotId);
        }

        Training training = trainingRepository.findById(slot.getTrainingId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Training not found"));
        requireMember(training.getClubId(), userId);
        requireMember(training.getClubId(), enrolledBy);

        trainingRepository.enroll(slotId, userId, enrolledBy);
    }

    @Transactional
    public void unenroll(UUID slotId, UUID userId) {
        trainingRepository.unenroll(slotId, userId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> listEnrollments(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Slot not found: " + slotId));
        Training training = trainingRepository.findById(slot.getTrainingId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Training not found"));
        requireMember(training.getClubId(), userId);
        return trainingRepository.listEnrollments(slotId);
    }

    @Transactional(readOnly = true)
    public List<Comment> listComments(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Slot not found: " + slotId));
        Training training = trainingRepository.findById(slot.getTrainingId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Training not found"));
        requireMember(training.getClubId(), userId);
        return trainingRepository.listComments(slotId);
    }

    @Transactional
    public Comment createComment(UUID slotId, String text, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Slot not found: " + slotId));
        Training training = trainingRepository.findById(slot.getTrainingId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Training not found"));
        requireMember(training.getClubId(), userId);
        UUID commentId = trainingRepository.createComment(slotId, userId, text);
        return Comment.builder()
                .id(commentId)
                .slotId(slotId)
                .userId(userId)
                .text(text)
                .build();
    }

    private void requireMember(UUID clubId, UUID userId) {
        if (!clubService.isMember(clubId, userId)) {
            throw new AccessDeniedException("No access to club " + clubId);
        }
    }

    private void requireOwner(UUID clubId, UUID userId) {
        clubService.get(clubId);
        if (!clubService.isMember(clubId, userId)) {
            throw new AccessDeniedException("No access to club " + clubId);
        }
    }
}
