package com.github.bogdanovmn.comeplay.club;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
class ClubComment {
    UUID id;
    UUID clubId;
    UUID userId;
    String authorName;
    String text;
    Instant createdAt;
}