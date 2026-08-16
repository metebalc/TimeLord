package com.timelord.common.state;

import com.timelord.common.model.TemporalPosition;

import java.util.Objects;
import java.util.UUID;

/** Render-neutral client state for one rewind trail. */
public record TimeRewindEffectState(
        UUID playerId,
        TemporalPosition origin,
        TemporalPosition destination,
        int totalTicks,
        int remainingTicks
) {
    public TimeRewindEffectState {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        if (totalTicks <= 0)
            throw new IllegalArgumentException("totalTicks must be positive");
        if (remainingTicks < 0 || remainingTicks > totalTicks)
            throw new IllegalArgumentException("remainingTicks must be between zero and totalTicks");
    }

    public static TimeRewindEffectState start(
            UUID playerId,
            TemporalPosition origin,
            TemporalPosition destination,
            int durationTicks
    ) {
        return new TimeRewindEffectState(playerId, origin, destination, durationTicks, durationTicks);
    }

    public TimeRewindEffectState tick() {
        return remainingTicks == 0
                ? this
                : new TimeRewindEffectState(playerId, origin, destination, totalTicks, remainingTicks - 1);
    }

    public boolean expired() {
        return remainingTicks == 0;
    }

    public float progress() {
        return 1.0F - remainingTicks / (float) totalTicks;
    }
}
