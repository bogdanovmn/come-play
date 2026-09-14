package com.github.bogdanovmn.comeplay.club;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
class InvitationInfo {
    UUID id;
    UUID clubId;
    String clubName;
    String name;
}