package com.timelord.common.logic;

import com.timelord.common.ability.AbilityId;
import com.timelord.common.state.CooldownState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative cooldown deadlines with no clock or networking dependency. */
public final class CooldownTracker {
    private final Map<UUID, EnumMap<AbilityId, Long>> readyAtTicks = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, AbilityId ability, long currentTick) {
        return remainingTicks(playerId, ability, currentTick) > 0;
    }

    public int remainingTicks(UUID playerId, AbilityId ability, long currentTick) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(ability, "ability");

        long readyAt = readyAtTicks
                .getOrDefault(playerId, new EnumMap<>(AbilityId.class))
                .getOrDefault(ability, 0L);

        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, readyAt - currentTick));
    }

    public CooldownState start(UUID playerId, AbilityId ability, long currentTick) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(ability, "ability");

        int duration = ability.cooldownTicks();
        if (duration > 0) {
            readyAtTicks
                    .computeIfAbsent(playerId, ignored -> new EnumMap<>(AbilityId.class))
                    .put(ability, currentTick + duration);
        }

        return new CooldownState(ability, duration, duration);
    }

    public Map<AbilityId, CooldownState> snapshot(UUID playerId, long currentTick) {
        Objects.requireNonNull(playerId, "playerId");
        EnumMap<AbilityId, CooldownState> snapshot = new EnumMap<>(AbilityId.class);
        EnumMap<AbilityId, Long> cooldowns = readyAtTicks.get(playerId);

        if (cooldowns == null)
            return Map.of();

        for (AbilityId ability : cooldowns.keySet()) {
            int remaining = remainingTicks(playerId, ability, currentTick);
            if (remaining > 0)
                snapshot.put(ability, new CooldownState(ability, remaining, ability.cooldownTicks()));
        }

        return Map.copyOf(snapshot);
    }

    public void clear(UUID playerId) {
        readyAtTicks.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public void clear() {
        readyAtTicks.clear();
    }
}
