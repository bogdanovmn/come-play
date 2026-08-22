package com.github.bogdanovmn.comeplay.user;

import com.github.bogdanovmn.comeplay.common.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    UserProfile getProfile(@CurrentUserId UUID userId) {
        return userService.getProfile(userId);
    }

    @PutMapping("/me")
    void updateProfile(@Valid @RequestBody UpdateProfileRequest request, @CurrentUserId UUID userId) {
        userService.updateDisplayName(userId, request.getDisplayName());
    }

    @GetMapping("/me/friends")
    List<FriendBrief> listFriends(@CurrentUserId UUID userId) {
        return userService.listFriends(userId);
    }

    @PostMapping("/me/friends")
    @ResponseStatus(HttpStatus.CREATED)
    void addFriend(@Valid @RequestBody AddFriendRequest request, @CurrentUserId UUID userId) {
        userService.addFriend(userId, request.getUserId());
    }

    @DeleteMapping("/me/friends/{friendId}")
    void removeFriend(@PathVariable UUID friendId, @CurrentUserId UUID userId) {
        userService.removeFriend(userId, friendId);
    }

    @GetMapping("/search")
    List<UserProfile> search(@RequestParam String term, @CurrentUserId UUID userId) {
        return userService.search(term, userId);
    }
}
