package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.common.PlayerSkillRepository;
import com.github.bogdanovmn.comeplay.common.SkillLevel;
import com.github.bogdanovmn.comeplay.security.AccessManagement;
import com.github.bogdanovmn.comeplay.sport.SportType;
import com.github.bogdanovmn.comeplay.sport.SportTypeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
class ClubService {

    private final ClubRepository clubRepository;
    private final AccessManagement accessManagement;
    private final SportTypeService sportTypeService;
    private final PlayerSkillRepository playerSkillRepository;
    private final int clubCommentsLimit;

    ClubService(
        ClubRepository clubRepository,
        AccessManagement accessManagement,
        SportTypeService sportTypeService,
        PlayerSkillRepository playerSkillRepository,
        @Value("${comeplay.club-comments-limit}") int clubCommentsLimit
    ) {
        this.clubRepository = clubRepository;
        this.accessManagement = accessManagement;
        this.sportTypeService = sportTypeService;
        this.playerSkillRepository = playerSkillRepository;
        this.clubCommentsLimit = clubCommentsLimit;
    }

    @Transactional(readOnly = true)
    public List<ClubBrief> listByOwner(UUID userId) {
        return clubRepository.listByOwner(userId);
    }

    @Transactional(readOnly = true)
    public List<ClubBrief> listByMember(UUID userId) {
        return clubRepository.listByMember(userId);
    }

    @Transactional(readOnly = true)
    public Club get(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new NoSuchElementException("Club not found: " + clubId));
    }

    @Transactional
    public ClubBrief create(CreateClubRequest request, UUID ownerId) {
        int sportTypeId = request.getSportTypeId();
        String sportTypeName = sportTypeService.requireById(sportTypeId).getName();
        int scheduleDays = request.getScheduleDays() == null
                ? Club.DEFAULT_SCHEDULE_DAYS
                : request.getScheduleDays();
        UUID clubId = clubRepository.create(
                request.getName(),
                sportTypeId,
                request.getDescription(),
                scheduleDays,
                ownerId
        );
        clubRepository.addMember(clubId, ownerId);
        return ClubBrief.builder()
                .id(clubId)
                .name(request.getName())
                .sportTypeId(sportTypeId)
                .sportTypeName(sportTypeName)
                .membersCount(1)
                .build();
    }

    @Transactional
    public void update(UUID clubId, UpdateClubRequest request, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        sportTypeService.requireById(request.getSportTypeId());
        clubRepository.update(
                clubId,
                request.getName(),
                request.getSportTypeId(),
                request.getDescription(),
                request.getScheduleDays()
        );
    }

    @Transactional
    public void close(UUID clubId, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        clubRepository.close(clubId);
    }

    @Transactional
    public void open(UUID clubId, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        clubRepository.open(clubId);
    }

    @Transactional(readOnly = true)
    public List<ClubComment> listComments(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return clubRepository.listComments(clubId, clubCommentsLimit);
    }

    @Transactional
    public ClubComment createComment(UUID clubId, String text, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        UUID commentId = clubRepository.createComment(clubId, userId, text);
        return clubRepository.findCommentById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + commentId));
    }

    @Transactional
    public List<InvitationBrief> listInvitations(UUID clubId, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        return clubRepository.listInvitations(clubId);
    }

    @Transactional
    public Invitation createInvitation(UUID clubId, String name, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        UUID invitationId = clubRepository.createInvitation(clubId, name, userId);
        return clubRepository.findInvitationById(invitationId).orElseThrow();
    }

    @Transactional
    public void deleteInvitation(UUID clubId, UUID invitationId, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        clubRepository.softDeleteInvitation(invitationId);
    }

    @Transactional(readOnly = true)
    public InvitationInfo invitationInfo(UUID invitationId) {
        return clubRepository.findInvitationInfoById(invitationId)
                .orElseThrow(() -> new NoSuchElementException("Invitation is not active anymore: " + invitationId));
    }

    @Transactional
    public void joinByInvitation(UUID invitationId, UUID userId) {
        Invitation invitation = clubRepository.findInvitationById(invitationId)
                .orElseThrow(() -> new NoSuchElementException("Invitation not found: " + invitationId));
        if (!invitation.isActive()) {
            throw new NoSuchElementException("Invitation is not active anymore: " + invitationId);
        }
        clubRepository.addMember(invitation.getClubId(), userId);
        clubRepository.recordJoiner(invitation.getId(), userId);
    }

    @Transactional(readOnly = true)
    public List<InvitationJoiner> listJoiners(UUID invitationId, UUID userId) {
        Invitation invitation = clubRepository.findInvitationById(invitationId)
                .orElseThrow(() -> new NoSuchElementException("Invitation not found: " + invitationId));
        accessManagement.requireOwner(invitation.getClubId(), userId);
        return clubRepository.listInvitationJoiners(invitationId);
    }

    @Transactional(readOnly = true)
    public List<ClubMember> listMembers(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return clubRepository.listMembers(clubId);
    }

    @Transactional
    public void leave(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        accessManagement.requireNotOwner(clubId, userId);
        clubRepository.removeMember(clubId, userId);
    }

    @Transactional
    public void setMemberSkill(UUID clubId, UUID memberId, SkillLevel skill, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        accessManagement.requireMember(clubId, memberId);
        playerSkillRepository.setClubOverride(clubId, memberId, skill);
    }

    @Transactional
    public void clearMemberSkill(UUID clubId, UUID memberId, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        accessManagement.requireMember(clubId, memberId);
        playerSkillRepository.deleteClubOverride(clubId, memberId);
    }
}
