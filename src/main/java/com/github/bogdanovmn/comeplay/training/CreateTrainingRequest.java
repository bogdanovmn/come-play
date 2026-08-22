package com.github.bogdanovmn.comeplay.training;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Value
class CreateTrainingRequest {
    @NotBlank
    String sportType;

    DayOfWeek dayOfWeek;

    LocalTime startTime;

    LocalTime endTime;

    @Min(1)
    @Max(100)
    int maxPlayers;
}
