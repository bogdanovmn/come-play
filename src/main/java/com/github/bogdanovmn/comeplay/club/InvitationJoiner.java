package com.github.bogdanovmn.comeplay.club;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
class InvitationJoiner {
    UUID userId;
    String name;
    Instant registeredAt;
}