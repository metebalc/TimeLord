package com.timelord.client.state;

import com.timelord.ability.AbilityManager.AbilityType;

import java.util.EnumMap;
import java.util.Map;

public final class ClientAbilityState {
    private static final Map<AbilityType, Integer> COOLDOWNS = new EnumMap<>(AbilityType.class);
    private static final Map<AbilityType, ActiveState> ACTIVE_STATES = new EnumMap<>(AbilityType.class);

    private ClientAbilityState() {}

    public static void tick() {
        COOLDOWNS.replaceAll((ability, remaining) -> Math.max(0, remaining - 1));
        ACTIVE_STATES.replaceAll((ability, state) -> state.totalTicks() > 0
                ? new ActiveState(
                        state.active(),
                        Math.max(0, state.remainingTicks() - 1),
                        state.totalTicks(),
                        state.elapsedTicks() + 1
                )
                : new ActiveState(state.active(), 0, 0, state.elapsedTicks() + 1));
        ACTIVE_STATES.entrySet().removeIf(entry -> entry.getValue().totalTicks() > 0
                && entry.getValue().remainingTicks() <= 0);
    }

    public static void setCooldown(AbilityType ability, int ticks) {
        COOLDOWNS.put(ability, Math.max(0, ticks));
    }

    public static int getCooldown(AbilityType ability) {
        return COOLDOWNS.getOrDefault(ability, 0);
    }

    public static float getCooldownProgress(AbilityType ability) {
        if (ability.cooldownTicks() <= 0)
            return 0.0F;

        return Math.min(1.0F, getCooldown(ability) / (float) ability.cooldownTicks());
    }

    public static void setActive(AbilityType ability, boolean active, int remainingTicks, int totalTicks) {
        if (!active) {
            ACTIVE_STATES.remove(ability);
            return;
        }

        ACTIVE_STATES.put(ability, new ActiveState(true, remainingTicks, totalTicks, 0));
    }

    public static boolean isActive(AbilityType ability) {
        ActiveState state = ACTIVE_STATES.get(ability);
        return state != null && state.active();
    }

    public static int getActiveElapsedTicks(AbilityType ability) {
        ActiveState state = ACTIVE_STATES.get(ability);
        return state == null ? 0 : state.elapsedTicks();
    }

    public static void clear() {
        COOLDOWNS.clear();
        ACTIVE_STATES.clear();
    }

    private record ActiveState(boolean active, int remainingTicks, int totalTicks, int elapsedTicks) {}
}
