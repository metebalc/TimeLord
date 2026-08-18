package com.timelord.mih;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Compact authoritative state sent to 1.20.1 clients. */
public record MadeInHeavenSyncState(
        long generationId,
        MadeInHeavenState.Phase phase,
        int elapsedActiveTicks,
        int collapseElapsedTicks,
        int serverTick,
        boolean theWorldActive,
        Set<UUID> activeUsers
) {
    public MadeInHeavenSyncState {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(activeUsers, "activeUsers");
        elapsedActiveTicks = clamp(elapsedActiveTicks, 0, MadeInHeavenCurves.BUILDUP_TICKS);
        collapseElapsedTicks = clamp(collapseElapsedTicks, 0, MadeInHeavenCurves.COLLAPSE_TICKS);
        activeUsers = Collections.unmodifiableSet(new LinkedHashSet<>(activeUsers));
    }

    public static MadeInHeavenSyncState inactive(int serverTick) {
        return new MadeInHeavenSyncState(
                0L,
                MadeInHeavenState.Phase.INACTIVE,
                0,
                0,
                serverTick,
                false,
                Set.of()
        );
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
