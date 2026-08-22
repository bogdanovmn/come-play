package com.github.bogdanovmn.comeplay.club;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
class CreateInvitationRequest {
    @NotBlank
    @Size(max = 200)
    String name;
}
