package com.github.bogdanovmn.comeplay.user;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class FriendBrief {
    UUID id;
    String displayName;
}
