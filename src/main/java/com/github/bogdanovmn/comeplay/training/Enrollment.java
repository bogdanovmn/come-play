package com.github.bogdanovmn.comeplay.training;

import com.github.bogdanovmn.comeplay.common.SkillLevel;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
class Enrollment {
    UUID slotId;
    UUID userId;
    UUID friendId;
    String name;
    UUID enrolledBy;
    Instant enrolledAt;
    boolean comingLater;
    SkillLevel skill;
    boolean owner;
    boolean waitlist;
}
