package com.github.bogdanovmn.comeplay.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ensures that every authenticated user has a local {@code app_user} record.
 * <p>
 * A user is identified by the ID from the JWT, so the local record is created lazily.
 * Doing it on every authenticated request (instead of only in the profile API) prevents
 * foreign key violations in endpoints that are called before the profile is loaded.
 */
@Component
@RequiredArgsConstructor
public class AppUserRegistrar {

    private final UserRepository userRepository;

    @Cacheable(value = "registeredUser", key = "#userId")
    public boolean ensureRegistered(UUID userId, String displayName) {
        userRepository.getOrCreate(userId, displayName);
        return true;
    }
}
