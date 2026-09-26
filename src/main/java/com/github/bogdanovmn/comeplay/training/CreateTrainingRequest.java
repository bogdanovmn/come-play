package com.github.bogdanovmn.comeplay.training;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Value
@Builder
class CreateTrainingRequest {
    @NotNull
    DayOfWeek dayOfWeek;

    @NotNull
    LocalTime startTime;

    @NotNull
    LocalTime endTime;

    @Min(1)
    @Max(100)
    int maxPlayers;

    @Size(max = 1000)
    String features;

    boolean oneOff;
}
