package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.common.SkillLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
class SetMemberSkillRequest {
    @NotNull
    SkillLevel skill;
}