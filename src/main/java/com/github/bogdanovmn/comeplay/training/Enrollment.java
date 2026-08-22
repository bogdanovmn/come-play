package com.github.bogdanovmn.comeplay.training;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class Enrollment {
    UUID slotId;
    UUID userId;
    UUID enrolledBy;
    Instant enrolledAt;
}
