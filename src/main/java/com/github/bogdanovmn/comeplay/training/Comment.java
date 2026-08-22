package com.github.bogdanovmn.comeplay.training;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class Comment {
    UUID id;
    UUID slotId;
    UUID userId;
    String text;
    Instant createdAt;
}
