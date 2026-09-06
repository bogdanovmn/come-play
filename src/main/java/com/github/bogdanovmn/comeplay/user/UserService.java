package com.github.bogdanovmn.comeplay.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
        return userRepository.getOrCreate(userId);
    }

    @Cacheable(value = "userProfile", key = "#userId")
    @Transactional
    public UserProfile getProfile(UUID userId) {
        return userRepository.getOrCreate(userId);
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
