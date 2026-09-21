package com.github.bogdanovmn.comeplay.user;

import com.github.bogdanovmn.comeplay.common.PlayerSkill;
import com.github.bogdanovmn.comeplay.common.PlayerSkillRepository;
import com.github.bogdanovmn.comeplay.common.SkillLevel;
import com.github.bogdanovmn.comeplay.sport.SportTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final PlayerSkillRepository playerSkillRepository;
    private final SportTypeService sportTypeService;

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
    @CacheEvict(value = "userProfile", key = "#userId")
    public void updateSettings(UUID userId, SaveProfileRequest request) {
        userRepository.updateDisplayName(userId, request.getDisplayName());
        for (SportSkillUpdate update : request.getSportSkills()) {
            sportTypeService.requireById(update.getSportTypeId());
            SkillLevel skill = update.getSkill();
            if (skill == null) {
                playerSkillRepository.deleteUserSkill(userId, update.getSportTypeId());
            } else {
                playerSkillRepository.setUserSkill(userId, update.getSportTypeId(), skill);
            }
        }
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

    @Transactional(readOnly = true)
    public List<PlayerSkill> listSkills(UUID userId) {
        return playerSkillRepository.userSkills(userId);
    }

    private String jwtUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}
