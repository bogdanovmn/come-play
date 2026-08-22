package com.github.bogdanovmn.comeplay.history;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class VisitByPlayer {
    UUID userId;
    int visitCount;
}
