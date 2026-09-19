package com.github.bogdanovmn.comeplay.training;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
class ComingLaterRequest {
    @NotNull
    Boolean comingLater;
}