package com.github.bogdanovmn.comeplay.training;

import lombok.Builder;
import lombok.Value;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Value
@Builder
class Training {
    UUID id;
    UUID clubId;
    DayOfWeek dayOfWeek;
    LocalTime startTime;
    LocalTime endTime;
    int maxPlayers;
    String features;
}
