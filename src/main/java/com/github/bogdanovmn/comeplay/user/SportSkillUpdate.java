package com.github.bogdanovmn.comeplay.user;

import com.github.bogdanovmn.comeplay.common.SkillLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
class SportSkillUpdate {
    @NotNull
    Integer sportTypeId;

    SkillLevel skill;
}