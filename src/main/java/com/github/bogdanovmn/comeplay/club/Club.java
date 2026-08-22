package com.github.bogdanovmn.comeplay.club;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
class Club {
    UUID id;
    String name;
    UUID ownerId;
    boolean closed;
    Instant createdAt;
}
