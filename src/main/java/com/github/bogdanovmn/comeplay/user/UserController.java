package com.github.bogdanovmn.comeplay.user;

import com.github.bogdanovmn.comeplay.common.PlayerSkill;
import com.github.bogdanovmn.comeplay.infrastructure.security.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    UserProfile getProfile(@CurrentUserId UUID userId) {
        return userService.getProfile(userId);
    }

    @PutMapping("/settings")
    void saveProfile(@Valid @RequestBody SaveProfileRequest request, @CurrentUserId UUID userId) {
        userService.updateSettings(userId, request);
    }

    @GetMapping("/sport-skills")
    List<PlayerSkill> listSkills(@CurrentUserId UUID userId) {
        return userService.listSkills(userId);
    }

    @GetMapping("/friends")
    List<FriendBrief> listFriends(@CurrentUserId UUID userId) {
        return userService.listFriends(userId);
    }

    @PostMapping("/friends")
    @ResponseStatus(HttpStatus.CREATED)
    void addFriend(@Valid @RequestBody AddFriendRequest request, @CurrentUserId UUID userId) {
        userService.addFriend(userId, request.getName());
    }

    @DeleteMapping("/friends/{friendId}")
    void removeFriend(@PathVariable UUID friendId, @CurrentUserId UUID userId) {
        userService.removeFriend(userId, friendId);
    }
}
