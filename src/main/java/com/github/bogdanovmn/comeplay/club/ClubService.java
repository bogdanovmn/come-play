package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.security.AccessManagement;
import com.github.bogdanovmn.comeplay.sport.SportType;
import com.github.bogdanovmn.comeplay.sport.SportTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class ClubService {

    private final ClubRepository clubRepository;
    private final AccessManagement accessManagement;
    private final SportTypeService sportTypeService;

    @Transactional(readOnly = true)
    @Cacheable(value = "clubs", key = "#userId")
    public List<ClubBrief> listByOwner(UUID userId) {
        return clubRepository.listByOwner(userId);
    }

    @Transactional(readOnly = true)
    public List<ClubBrief> listByMember(UUID userId) {
        return clubRepository.listByMember(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "club", key = "#clubId")
    public Club get(UUID clubId, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new NoSuchElementException("Club not found: " + clubId));
    }

    @Transactional
    @CacheEvict(value = "clubs", key = "#ownerId")
    public ClubBrief create(String name, int sportTypeId, String description, UUID ownerId) {
        String sportTypeName = sportTypeService.requireById(sportTypeId).getName();
        UUID clubId = clubRepository.create(name, sportTypeId, description, ownerId);
        clubRepository.addMember(clubId, ownerId);
        return ClubBrief.builder()
                .id(clubId)
                .name(name)
                .sportTypeId(sportTypeId)
                .sportTypeName(sportTypeName)
                .membersCount(1)
                .build();
    }

    @Transactional
    @CacheEvict(value = {"club", "clubs"}, key = "#clubId")
    public void update(UUID clubId, String name, int sportTypeId, String description, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        sportTypeService.requireById(sportTypeId);
        clubRepository.update(clubId, name, sportTypeId, description);
    }

    @Transactional
    @CacheEvict(value = {"club", "clubs"}, key = "#clubId")
    public void close(UUID clubId, UUID userId) {
        accessManagement.requireOwner(clubId, userId);
        clubRepository.close(clubId);
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

    @Transactional(readOnly = true)
    public InvitationInfo invitationInfo(UUID invitationId) {
        return clubRepository.findInvitationInfoById(invitationId)
                .orElseThrow(() -> new NoSuchElementException("Invitation not found: " + invitationId));
    }

    @Transactional
    public void joinByInvitation(UUID invitationId, UUID userId) {
        Invitation invitation = clubRepository.findInvitationById(invitationId)
                .orElseThrow(() -> new NoSuchElementException("Invitation not found: " + invitationId));
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
}
