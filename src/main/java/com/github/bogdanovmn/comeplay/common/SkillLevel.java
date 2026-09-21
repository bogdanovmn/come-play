package com.github.bogdanovmn.comeplay.common;

public enum SkillLevel {
    BEGINNER("Начинающий"),
    INTERMEDIATE("Продолжающий"),
    ADVANCED("Продвинутый"),
    EXPERT("Эксперт");

    private final String label;

    SkillLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}