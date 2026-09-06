package com.github.bogdanovmn.comeplay.training;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Value;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Value
@Builder
class CreateTrainingRequest {
    DayOfWeek dayOfWeek;

    LocalTime startTime;

    LocalTime endTime;

    @Min(1)
    @Max(100)
    int maxPlayers;
}
