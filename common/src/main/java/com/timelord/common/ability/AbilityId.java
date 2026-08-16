package com.timelord.common.ability;

/**
 * Stable, Minecraft-independent identity and metadata for an ability.
 *
 * <p>The network IDs intentionally match the existing 1.20.1 protocol.</p>
 */
public enum AbilityId {
    SLOW_TIME(0, 8 * 20, "Slow Time", "ability.time-lord.slow_time", false),
    THE_WORLD(1, 0, "The World", "ability.time-lord.the_world", false),
    DIMENSION_CUT(2, 4 * 20, "The Judgement Cut", "ability.time-lord.dimension_cut", true),
    TIME_SHIFT(3, 0, "Time Shift", "ability.time-lord.time_shift", false),
    TIME_REWIND(4, 15 * 20, "Time Rewind", "ability.time-lord.time_rewind", false),
    FUTURE_SIGHT(5, 20 * 20, "Future Sight", "ability.time-lord.future_sight", false);

    private final int networkId;
    private final int cooldownTicks;
    private final String displayName;
    private final String translationKey;
    private final boolean chargeable;

    AbilityId(int networkId, int cooldownTicks, String displayName, String translationKey, boolean chargeable) {
        this.networkId = networkId;
        this.cooldownTicks = cooldownTicks;
        this.displayName = displayName;
        this.translationKey = translationKey;
        this.chargeable = chargeable;
    }

    public int networkId() {
        return networkId;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public String displayName() {
        return displayName;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean isChargeable() {
        return chargeable;
    }

    public static AbilityId fromNetworkId(int networkId) {
        for (AbilityId ability : values()) {
            if (ability.networkId == networkId)
                return ability;
        }

        return null;
    }
}
