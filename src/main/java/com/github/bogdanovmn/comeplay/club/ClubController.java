package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.infrastructure.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clubs")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
class ClubController {

    private final ClubService clubService;

    @GetMapping("/owned")
    List<ClubBrief> listOwned(@CurrentUserId UUID userId) {
        return clubService.listByOwner(userId);
    }

    @GetMapping("/member")
    List<ClubBrief> listMember(@CurrentUserId UUID userId) {
        return clubService.listByMember(userId);
    }

    @GetMapping("/{clubId}")
    Club get(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        return clubService.get(clubId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClubBrief create(@Valid @RequestBody CreateClubRequest request, @CurrentUserId UUID userId) {
        return clubService.create(request.getName(), request.getSportTypeId(), request.getDescription(), userId);
    }

    @PutMapping("/{clubId}")
    void update(@PathVariable UUID clubId, @Valid @RequestBody UpdateClubRequest request, @CurrentUserId UUID userId) {
        clubService.update(clubId, request.getName(), request.getSportTypeId(), request.getDescription(), userId);
    }

    @PutMapping("/{clubId}/close")
    void close(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        clubService.close(clubId, userId);
    }

    @PutMapping("/{clubId}/open")
    void open(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        clubService.open(clubId, userId);
    }

    @GetMapping("/{clubId}/invitations")
    List<InvitationBrief> listInvitations(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        return clubService.listInvitations(clubId, userId);
    }

    @PostMapping("/{clubId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    Invitation createInvitation(@PathVariable UUID clubId, @Valid @RequestBody CreateInvitationRequest request, @CurrentUserId UUID userId) {
        return clubService.createInvitation(clubId, request.getName(), userId);
    }

    @GetMapping("/{clubId}/invitations/{invitationId}/joiners")
    List<InvitationJoiner> listJoiners(@PathVariable UUID invitationId, @CurrentUserId UUID userId) {
        return clubService.listJoiners(invitationId, userId);
    }

    @DeleteMapping("/{clubId}/invitations/{invitationId}")
    void deleteInvitation(@PathVariable UUID clubId, @PathVariable UUID invitationId, @CurrentUserId UUID userId) {
        clubService.deleteInvitation(clubId, invitationId, userId);
    }

    @PostMapping("/invitations/{invitationId}/join")
    void joinByInvitation(@PathVariable UUID invitationId, @CurrentUserId UUID userId) {
        clubService.joinByInvitation(invitationId, userId);
    }

    @GetMapping("/invitations/{invitationId}")
    @PreAuthorize("permitAll()")
    InvitationInfo invitationInfo(@PathVariable UUID invitationId) {
        return clubService.invitationInfo(invitationId);
    }

    @GetMapping("/{clubId}/members")
    List<ClubMember> listMembers(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        return clubService.listMembers(clubId, userId);
    }

    @DeleteMapping("/{clubId}/members")
    void leave(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        clubService.leave(clubId, userId);
    }
}
