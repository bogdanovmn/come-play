package com.github.bogdanovmn.comeplay.user;

import lombok.Value;

import java.util.UUID;

@Value
public class AddFriendRequest {
    UUID userId;
}
