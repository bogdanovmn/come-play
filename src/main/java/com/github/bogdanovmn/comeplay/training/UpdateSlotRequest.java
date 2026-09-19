package com.github.bogdanovmn.comeplay.training;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.time.LocalTime;

@Value
class UpdateSlotRequest {
    @NotNull
    LocalTime startTime;

    @NotNull
    LocalTime endTime;

    @Min(1)
    @Max(100)
    int maxPlayers;

    @Size(max = 1000)
    String features;
}