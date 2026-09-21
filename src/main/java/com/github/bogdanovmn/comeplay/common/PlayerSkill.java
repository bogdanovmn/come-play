package com.github.bogdanovmn.comeplay.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlayerSkill {
    int sportTypeId;
    String sportTypeName;
    SkillLevel skill;
}