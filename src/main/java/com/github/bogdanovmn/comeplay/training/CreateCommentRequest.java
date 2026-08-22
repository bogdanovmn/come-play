package com.github.bogdanovmn.comeplay.training;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
class CreateCommentRequest {
    @NotBlank
    String text;
}
