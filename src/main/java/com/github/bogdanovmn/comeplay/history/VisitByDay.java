package com.github.bogdanovmn.comeplay.history;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class VisitByDay {
    LocalDate date;
    int visitCount;
}
