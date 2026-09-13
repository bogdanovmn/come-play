package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.infrastructure.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slots")
class TrainingSlotController {

    private final TrainingService trainingService;

    TrainingSlotController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/{slotId}/enroll")
    void enroll(
        @PathVariable UUID slotId,
        @RequestBody(required = false) EnrollRequest request,
        @CurrentUserId UUID userId
    ) {
        UUID friendId = request != null ? request.getFriendId() : null;
        if (friendId != null) {
            trainingService.enrollFriend(slotId, friendId, userId);
        } else {
            trainingService.enroll(slotId, userId, userId);
        }
    }

    @DeleteMapping("/{slotId}/enroll")
    void unenroll(
        @PathVariable UUID slotId,
        @RequestParam(required = false) UUID friendId,
        @CurrentUserId UUID userId
    ) {
        if (friendId != null) {
            trainingService.unenrollFriend(slotId, friendId, userId);
        } else {
            trainingService.unenroll(slotId, userId);
        }
    }

    @GetMapping("/{slotId}/enrollments")
    List<Enrollment> listEnrollments(@PathVariable UUID slotId, @CurrentUserId UUID userId) {
        return trainingService.listEnrollments(slotId, userId);
    }

    @GetMapping("/{slotId}/comments")
    List<Comment> listComments(@PathVariable UUID slotId, @CurrentUserId UUID userId) {
        return trainingService.listComments(slotId, userId);
    }

    @PostMapping("/{slotId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    Comment createComment(
        @PathVariable UUID slotId,
        @Valid @RequestBody CreateCommentRequest request,
        @CurrentUserId UUID userId
    ) {
        return trainingService.createComment(slotId, request.getText(), userId);
    }
}