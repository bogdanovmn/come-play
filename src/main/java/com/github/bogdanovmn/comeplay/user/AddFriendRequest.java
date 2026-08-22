package com.github.bogdanovmn.comeplay.user;

import lombok.Value;

import java.util.UUID;

@Value
class AddFriendRequest {
    UUID userId;
}
