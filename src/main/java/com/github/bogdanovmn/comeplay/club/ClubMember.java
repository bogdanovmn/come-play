package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.common.SkillLevel;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
class ClubMember {
    UUID id;
    String name;
    SkillLevel skill;
    SkillLevel ownSkill;
    boolean owner;
    boolean overridden;
}