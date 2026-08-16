package com.timelord.client.ability;

import com.timelord.ability.AbilityManager.AbilityType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AbilityBookCatalog {
    private static final Map<AbilityType, Entry> ENTRIES = new EnumMap<>(AbilityType.class);

    static {
        register(AbilityType.SLOW_TIME, AbilityCategory.TIME_CONTROL, "ability.time-lord.slow_time.description");
        register(AbilityType.THE_WORLD, AbilityCategory.TIME_CONTROL, "ability.time-lord.the_world.description");
        register(AbilityType.TIME_REWIND, AbilityCategory.TIME_CONTROL, "ability.time-lord.time_rewind.description");
        register(AbilityType.TIME_SHIFT, AbilityCategory.MOBILITY, "ability.time-lord.time_shift.description");
        register(AbilityType.FUTURE_SIGHT, AbilityCategory.PERCEPTION, "ability.time-lord.future_sight.description");
        register(AbilityType.DIMENSION_CUT, AbilityCategory.COMBAT, "ability.time-lord.dimension_cut.description");
    }

    private AbilityBookCatalog() {}

    public static Entry get(AbilityType ability) {
        return ENTRIES.get(ability);
    }

    public static List<Entry> getByCategory(AbilityCategory category) {
        return ENTRIES.values().stream()
                .filter(entry -> entry.category() == category)
                .toList();
    }

    private static void register(AbilityType ability, AbilityCategory category, String descriptionKey) {
        ENTRIES.put(ability, new Entry(ability, category, descriptionKey));
    }

    public record Entry(AbilityType ability, AbilityCategory category, String descriptionKey) {}
}
