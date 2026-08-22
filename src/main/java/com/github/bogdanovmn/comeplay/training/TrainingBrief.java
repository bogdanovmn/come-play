package com.github.bogdanovmn.comeplay.training;

import lombok.Builder;
import lombok.Value;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Value
@Builder
public class TrainingBrief {
    UUID id;
    String sportType;
    DayOfWeek dayOfWeek;
    LocalTime startTime;
    LocalTime endTime;
    int maxPlayers;
}
