package com.github.bogdanovmn.comeplay.training;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class CreateCommentRequest {
    @NotBlank
    String text;
}
