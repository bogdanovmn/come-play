package com.github.bogdanovmn.comeplay.training;

import lombok.Builder;
import lombok.Value;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Value
@Builder
class TrainingSlot {
    UUID id;
    UUID trainingId;
    UUID clubId;
    String clubName;
    LocalDate slotDate;
    DayOfWeek dayOfWeek;
    LocalTime startTime;
    LocalTime endTime;
    int enrolledCount;
    int maxPlayers;
    int commentsCount;
    String features;
    boolean overridden;
}
