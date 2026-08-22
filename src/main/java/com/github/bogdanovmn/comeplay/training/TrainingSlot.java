package com.github.bogdanovmn.comeplay.training;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
class TrainingSlot {
    UUID id;
    UUID trainingId;
    LocalDate slotDate;
    int enrolledCount;
    int maxPlayers;
}
