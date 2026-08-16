package com.timelord.common.state;

/** Client- or server-side ability activity expressed only in ticks. */
public record AbilityState(
        boolean active,
        int remainingTicks,
        int totalTicks,
        int elapsedTicks
) {
    public AbilityState {
        if (remainingTicks < 0)
            throw new IllegalArgumentException("remainingTicks must not be negative");
        if (totalTicks < 0)
            throw new IllegalArgumentException("totalTicks must not be negative");
        if (elapsedTicks < 0)
            throw new IllegalArgumentException("elapsedTicks must not be negative");
        if (totalTicks > 0 && remainingTicks > totalTicks)
            throw new IllegalArgumentException("remainingTicks must not exceed totalTicks");
    }

    public static AbilityState inactive() {
        return new AbilityState(false, 0, 0, 0);
    }

    public static AbilityState activeIndefinitely() {
        return new AbilityState(true, 0, 0, 0);
    }

    public static AbilityState activeFor(int totalTicks) {
        if (totalTicks <= 0)
            throw new IllegalArgumentException("totalTicks must be positive");

        return new AbilityState(true, totalTicks, totalTicks, 0);
    }

    public AbilityState tick() {
        if (!active)
            return this;

        if (totalTicks == 0)
            return new AbilityState(true, 0, 0, elapsedTicks + 1);

        int remaining = Math.max(0, remainingTicks - 1);
        return new AbilityState(remaining > 0, remaining, totalTicks, elapsedTicks + 1);
    }
}
