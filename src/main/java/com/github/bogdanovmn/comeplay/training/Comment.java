package com.github.bogdanovmn.comeplay.training;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
class Comment {
    UUID id;
    UUID slotId;
    UUID userId;
    String authorName;
    String text;
    Instant createdAt;
}
