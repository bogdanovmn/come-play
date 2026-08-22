package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.infrastructure.security.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clubs")
class ClubController {

    private final ClubService clubService;

    ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

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
        if (!clubService.isMember(clubId, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("No access");
        }
        return clubService.get(clubId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClubBrief create(@Valid @RequestBody CreateClubRequest request, @CurrentUserId UUID userId) {
        return clubService.create(request.getName(), userId);
    }

    @PutMapping("/{clubId}")
    void update(@PathVariable UUID clubId, @Valid @RequestBody UpdateClubRequest request, @CurrentUserId UUID userId) {
        clubService.update(clubId, request.getName(), userId);
    }

    @PutMapping("/{clubId}/close")
    void close(@PathVariable UUID clubId, @CurrentUserId UUID userId) {
        clubService.close(clubId, userId);
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

    @PostMapping("/invitations/{invitationId}/join")
    void joinByInvitation(@PathVariable UUID invitationId, @CurrentUserId UUID userId) {
        clubService.joinByInvitation(invitationId, userId);
    }
}
