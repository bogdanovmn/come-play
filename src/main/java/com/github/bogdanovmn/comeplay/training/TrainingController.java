package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.common.CurrentUserId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clubs/{clubId}/trainings")
class TrainingController {

    private final TrainingService trainingService;

    TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @GetMapping
    List<TrainingBrief> list(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        return trainingService.listByClub(clubId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TrainingBrief create(
            @PathVariable UUID clubId,
            @Valid @RequestBody CreateTrainingRequest request,
            @CurrentUserId UUID userId
    ) {
        return trainingService.create(clubId, request, userId);
    }

    @DeleteMapping("/{trainingId}")
    void delete(@PathVariable UUID clubId, @PathVariable UUID trainingId, @CurrentUserId UUID userId) {
        trainingService.delete(trainingId, userId);
    }

    @GetMapping("/{trainingId}/slots")
    List<TrainingSlot> listSlots(
            @PathVariable UUID clubId,
            @PathVariable UUID trainingId,
            @CurrentUserId UUID userId
    ) {
        return trainingService.listSlotsByTraining(trainingId, userId);
    }

    @GetMapping("/slots")
    List<TrainingSlot> listAllSlots(
            @PathVariable UUID clubId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUserId UUID userId
    ) {
        return trainingService.listSlots(clubId, from, to, userId);
    }

    @PostMapping("/slots/{slotId}/enroll")
    void enroll(
            @PathVariable UUID clubId,
            @PathVariable UUID slotId,
            @RequestBody(required = false) EnrollRequest request,
            @CurrentUserId UUID userId
    ) {
        UUID targetUserId = (request != null && request.getUserId() != null) ? request.getUserId() : userId;
        trainingService.enroll(slotId, targetUserId, userId);
    }

    @DeleteMapping("/slots/{slotId}/enroll")
    void unenroll(@PathVariable UUID clubId, @PathVariable UUID slotId, @CurrentUserId UUID userId) {
        trainingService.unenroll(slotId, userId);
    }

    @GetMapping("/slots/{slotId}/enrollments")
    List<Enrollment> listEnrollments(@PathVariable UUID clubId, @PathVariable UUID slotId, @CurrentUserId UUID userId) {
        return trainingService.listEnrollments(slotId, userId);
    }

    @GetMapping("/slots/{slotId}/comments")
    List<Comment> listComments(@PathVariable UUID clubId, @PathVariable UUID slotId, @CurrentUserId UUID userId) {
        return trainingService.listComments(slotId, userId);
    }

    @PostMapping("/slots/{slotId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    Comment createComment(
            @PathVariable UUID clubId,
            @PathVariable UUID slotId,
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUserId UUID userId
    ) {
        return trainingService.createComment(slotId, request.getText(), userId);
    }
}
