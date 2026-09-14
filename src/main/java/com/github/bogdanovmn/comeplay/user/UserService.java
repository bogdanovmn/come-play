package com.github.bogdanovmn.comeplay.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserProfile getOrCreate(UUID userId) {
        return userRepository.getOrCreate(userId, jwtUserName());
    }

    @Cacheable(value = "userProfile", key = "#userId")
    @Transactional
    public UserProfile getProfile(UUID userId) {
        return userRepository.getOrCreate(userId, jwtUserName());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userProfile", key = "#userId"),
            @CacheEvict(value = {"club", "clubs"}, allEntries = true)
    })
    public void updateDisplayName(UUID userId, String displayName) {
        userRepository.updateDisplayName(userId, displayName);
    }

    @Transactional(readOnly = true)
    public List<FriendBrief> listFriends(UUID userId) {
        return userRepository.listFriends(userId);
    }

    @Transactional
    public void addFriend(UUID userId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Friend name is required");
        }
        userRepository.createFriend(userId, name.trim());
    }

    @Transactional
    public void removeFriend(UUID userId, UUID friendId) {
        userRepository.deleteFriend(userId, friendId);
    }

    private String jwtUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}
