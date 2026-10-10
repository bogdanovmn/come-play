package com.github.bogdanovmn.comeplay.club;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
class UpdateClubRequest {
    @NotBlank
    @Size(max = 200)
    String name;

    @NotNull
    @Positive
    Integer sportTypeId;

    @Size(max = 2000)
    String description;

    @NotNull
    @Min(Club.MIN_SCHEDULE_DAYS)
    @Max(Club.MAX_SCHEDULE_DAYS)
    Integer scheduleDays;
}
