package com.timelord.client.ability;

public enum AbilityCategory {
    TIME_CONTROL("category.time-lord.time_control"),
    MOBILITY("category.time-lord.mobility"),
    PERCEPTION("category.time-lord.perception"),
    COMBAT("category.time-lord.combat");

    private final String translationKey;

    AbilityCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
