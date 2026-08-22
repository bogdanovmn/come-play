package com.github.bogdanovmn.comeplay.club;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class InvitationBrief {
    UUID id;
    String name;
    int joinedCount;
}
