package com.github.bogdanovmn.comeplay.sport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
class CreateSportTypeRequest {
    @NotBlank
    @Size(max = 50)
    String name;
}