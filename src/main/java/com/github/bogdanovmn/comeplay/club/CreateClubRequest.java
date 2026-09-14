package com.github.bogdanovmn.comeplay.club;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
class CreateClubRequest {
    @NotBlank
    @Size(max = 200)
    String name;

    @NotNull
    @Positive
    Integer sportTypeId;

    @Size(max = 2000)
    String description;
}
