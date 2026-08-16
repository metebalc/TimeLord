package com.timelord.common.state;

import com.timelord.common.ability.AbilityId;

import java.util.Objects;

/** Remaining and total cooldown duration for one ability. */
public record CooldownState(AbilityId ability, int remainingTicks, int totalTicks) {
    public CooldownState {
        Objects.requireNonNull(ability, "ability");
        if (remainingTicks < 0)
            throw new IllegalArgumentException("remainingTicks must not be negative");
        if (totalTicks < 0)
            throw new IllegalArgumentException("totalTicks must not be negative");
        if (remainingTicks > totalTicks)
            throw new IllegalArgumentException("remainingTicks must not exceed totalTicks");
    }

    public static CooldownState start(AbilityId ability) {
        return new CooldownState(ability, ability.cooldownTicks(), ability.cooldownTicks());
    }

    public boolean ready() {
        return remainingTicks == 0;
    }

    public double progress() {
        return totalTicks == 0 ? 0.0D : remainingTicks / (double) totalTicks;
    }

    public CooldownState tick() {
        if (ready())
            return this;

        return new CooldownState(ability, remainingTicks - 1, totalTicks);
    }
}
