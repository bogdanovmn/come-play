package com.github.bogdanovmn.comeplay.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
class UpdateProfileRequest {
    @NotBlank
    @Size(max = 100)
    String displayName;
}
