package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.security.AccessManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TrainingService {

    private final TrainingRepository trainingRepository;
    private final AccessManagement accessManagement;

    @Transactional(readOnly = true)
    public List<TrainingBrief> listByClub(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return trainingRepository.listByClub(clubId);
    }

    @Transactional
    public TrainingBrief create(UUID clubId, CreateTrainingRequest request, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
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
            .orElseThrow(() -> new NoSuchElementException("Training not found: " + trainingId));
        accessManagement.requireOwner(training.getClubId(), userId);
        trainingRepository.delete(trainingId);
    }

    @Transactional(readOnly = true)
    public List<TrainingSlot> listSlots(UUID clubId, LocalDate from, LocalDate to, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return trainingRepository.listSlots(clubId, from, to);
    }

    @Transactional(readOnly = true)
    public List<TrainingSlot> listSlotsByTraining(UUID trainingId, UUID userId) {
        Training training = trainingRepository.findById(trainingId)
            .orElseThrow(() -> new NoSuchElementException("Training not found: " + trainingId));
        accessManagement.requireMember(training.getClubId(), userId);
        return trainingRepository.listSlotsByTraining(trainingId);
    }

    @Transactional
    public void enroll(UUID slotId, UUID userId, UUID enrolledBy) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));

        if (slot.getEnrolledCount() >= slot.getMaxPlayers()) {
            throw new IllegalArgumentException("Slot is full: " + slotId);
        }

        Training training = trainingRepository.findById(slot.getTrainingId())
            .orElseThrow(() -> new NoSuchElementException("Training not found"));
        accessManagement.requireMember(training.getClubId(), userId);
        accessManagement.requireMember(training.getClubId(), enrolledBy);

        trainingRepository.enroll(slotId, userId, enrolledBy);
    }

    @Transactional
    public void unenroll(UUID slotId, UUID userId) {
        trainingRepository.unenroll(slotId, userId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> listEnrollments(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        Training training = trainingRepository.findById(slot.getTrainingId())
            .orElseThrow(() -> new NoSuchElementException("Training not found"));
        accessManagement.requireMember(training.getClubId(), userId);
        return trainingRepository.listEnrollments(slotId);
    }

    @Transactional(readOnly = true)
    public List<Comment> listComments(UUID slotId, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        Training training = trainingRepository.findById(slot.getTrainingId())
            .orElseThrow(() -> new NoSuchElementException("Training not found"));
        accessManagement.requireMember(training.getClubId(), userId);
        return trainingRepository.listComments(slotId);
    }

    @Transactional
    public Comment createComment(UUID slotId, String text, UUID userId) {
        TrainingSlot slot = trainingRepository.findSlotById(slotId)
            .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));
        Training training = trainingRepository.findById(slot.getTrainingId())
            .orElseThrow(() -> new NoSuchElementException("Training not found"));
        accessManagement.requireMember(training.getClubId(), userId);
        UUID commentId = trainingRepository.createComment(slotId, userId, text);
        return Comment.builder()
            .id(commentId)
            .slotId(slotId)
            .userId(userId)
            .text(text)
        .build();
    }
}
