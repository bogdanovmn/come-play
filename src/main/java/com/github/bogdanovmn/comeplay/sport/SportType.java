package com.github.bogdanovmn.comeplay.sport;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SportType {
    int id;
    String name;
}