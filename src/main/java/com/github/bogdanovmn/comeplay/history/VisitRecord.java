package com.github.bogdanovmn.comeplay.history;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
class VisitRecord {
    UUID id;
    UUID clubId;
    UUID userId;
    LocalDate slotDate;
    String sportType;
}
