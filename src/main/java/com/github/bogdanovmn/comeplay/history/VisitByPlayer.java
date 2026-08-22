package com.github.bogdanovmn.comeplay.history;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
class VisitByPlayer {
    UUID userId;
    int visitCount;
}
