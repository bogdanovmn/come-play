package com.github.bogdanovmn.comeplay.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.util.List;

@Value
class SaveProfileRequest {
    @NotBlank
    @Size(max = 100)
    String displayName;

    List<SportSkillUpdate> sportSkills;
}