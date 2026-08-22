package com.github.bogdanovmn.comeplay.user;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
class UserService {

    private final UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserProfile getOrCreate(UUID userId) {
        return userRepository.getOrCreate(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userProfile", key = "#userId")
    public UserProfile getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + userId));
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public void updateDisplayName(UUID userId, String displayName) {
        userRepository.updateDisplayName(userId, displayName);
    }

    @Transactional(readOnly = true)
    public List<FriendBrief> listFriends(UUID userId) {
        return userRepository.listFriends(userId);
    }

    @Transactional
    public void addFriend(UUID userId, UUID friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Cannot add yourself as friend");
        }
        userRepository.getOrCreate(friendId);
        userRepository.addFriend(userId, friendId);
    }

    @Transactional
    public void removeFriend(UUID userId, UUID friendId) {
        userRepository.removeFriend(userId, friendId);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> search(String term, UUID userId) {
        return userRepository.searchByDisplayName(term, userId);
    }
}
