package com.github.bogdanovmn.comeplay.club;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ClubService {

    private final ClubRepository clubRepository;

    ClubService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

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
    public Club get(UUID clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new NoSuchElementException("Club not found: " + clubId));
    }

    @Transactional
    @CacheEvict(value = "clubs", key = "#ownerId")
    public ClubBrief create(String name, UUID ownerId) {
        UUID clubId = clubRepository.create(name, ownerId);
        clubRepository.addMember(clubId, ownerId);
        return ClubBrief.builder()
                .id(clubId)
                .name(name)
                .membersCount(1)
                .build();
    }

    @Transactional
    @CacheEvict(value = {"club", "clubs"}, key = "#clubId")
    public void update(UUID clubId, String name, UUID userId) {
        requireOwner(clubId, userId);
        clubRepository.update(clubId, name);
    }

    @Transactional
    @CacheEvict(value = {"club", "clubs"}, key = "#clubId")
    public void close(UUID clubId, UUID userId) {
        requireOwner(clubId, userId);
        clubRepository.close(clubId);
    }

    @Transactional
    public List<InvitationBrief> listInvitations(UUID clubId, UUID userId) {
        requireOwner(clubId, userId);
        return clubRepository.listInvitations(clubId);
    }

    @Transactional
    public Invitation createInvitation(UUID clubId, String name, UUID userId) {
        requireOwner(clubId, userId);
        UUID invitationId = clubRepository.createInvitation(clubId, name, userId);
        return clubRepository.findInvitationById(invitationId).orElseThrow();
    }

    @Transactional
    public void joinByInvitation(UUID invitationId, UUID userId) {
        Invitation invitation = clubRepository.findInvitationById(invitationId)
                .orElseThrow(() -> new NoSuchElementException("Invitation not found: " + invitationId));
        clubRepository.addMember(invitation.getClubId(), userId);
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID clubId, UUID userId) {
        return clubRepository.isMember(clubId, userId);
    }

    private void requireOwner(UUID clubId, UUID userId) {
        if (!clubRepository.isOwner(clubId, userId)) {
            throw new AccessDeniedException("User " + userId + " is not owner of club " + clubId);
        }
    }
}
