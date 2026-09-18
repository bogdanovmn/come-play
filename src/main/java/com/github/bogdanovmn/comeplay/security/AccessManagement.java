package com.github.bogdanovmn.comeplay.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccessManagement {
    private final AccessManagementRepository accessManagementRepository;

    public void requireMember(UUID clubId, UUID userId) {
        if (!accessManagementRepository.isMember(clubId, userId)) {
            throw new AccessDeniedException("No access to club %s for member %s".formatted(clubId, userId));
        }
    }

    public void requireOwner(UUID clubId, UUID userId) {
        if (!accessManagementRepository.isOwner(clubId, userId)) {
            throw new AccessDeniedException("No access to club %s for owner %s".formatted(clubId, userId));
        }
    }

    public void requireNotOwner(UUID clubId, UUID userId) {
        if (accessManagementRepository.isOwner(clubId, userId)) {
            throw new AccessDeniedException("Club owner %s cannot leave own club %s".formatted(userId, clubId));
        }
    }
}
