package com.github.bogdanovmn.comeplay.club;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
class Club {
    static final int MIN_SCHEDULE_DAYS = 1;
    static final int MAX_SCHEDULE_DAYS = 14;
    static final int DEFAULT_SCHEDULE_DAYS = 3;

    UUID id;
    String name;
    int sportTypeId;
    String sportTypeName;
    UUID ownerId;
    String ownerName;
    String description;
    boolean closed;
    int scheduleDays;
    Instant createdAt;
}
