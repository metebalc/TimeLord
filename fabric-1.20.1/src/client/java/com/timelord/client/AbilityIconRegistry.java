package com.timelord.client;

import com.timelord.ability.AbilityManager.AbilityType;

import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class AbilityIconRegistry {
    private static final Map<AbilityType, Identifier> ICONS = new EnumMap<>(AbilityType.class);

    static {
        ICONS.put(AbilityType.SLOW_TIME, icon("domain_32x32.png"));
        ICONS.put(AbilityType.THE_WORLD, icon("hourglass_32x32.png"));
        ICONS.put(AbilityType.DIMENSION_CUT, icon("cuts_32x32.png"));
        ICONS.put(AbilityType.TIME_SHIFT, icon("dash_32x32.png"));
        ICONS.put(AbilityType.TIME_REWIND, icon("rewind_32x32.png"));
        ICONS.put(AbilityType.FUTURE_SIGHT, icon("future_sight_32x32.png"));
        ICONS.put(AbilityType.MADE_IN_HEAVEN, icon("mih_32x32.png"));
    }

    private AbilityIconRegistry() {}

    public static Identifier get(AbilityType ability) {
        return ICONS.get(ability);
    }

    private static Identifier icon(String fileName) {
        return new Identifier("time-lord", "textures/" + fileName);
    }
}
