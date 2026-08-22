package com.github.bogdanovmn.comeplay.club;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
class ClubBrief {
    UUID id;
    String name;
    int membersCount;
}
