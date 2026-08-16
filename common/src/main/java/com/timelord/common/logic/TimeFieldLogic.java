package com.timelord.common.logic;

import com.timelord.common.model.TemporalPosition;
import com.timelord.common.state.SlowFieldState;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/** Pure entity tick scheduling for overlapping slow-time fields. */
public final class TimeFieldLogic {
    private TimeFieldLogic() {}

    public static int tickInterval(
            Collection<SlowFieldState> fields,
            String dimensionId,
            UUID entityId,
            TemporalPosition entityPosition
    ) {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(entityPosition, "entityPosition");

        int largestInterval = 1;
        for (SlowFieldState field : fields) {
            if (field.contains(dimensionId, entityId, entityPosition))
                largestInterval = Math.max(largestInterval, field.tickInterval());
        }

        return largestInterval;
    }

    public static boolean shouldTick(long serverTick, int interval) {
        if (interval <= 0)
            throw new IllegalArgumentException("interval must be positive");

        return interval == 1 || serverTick % interval == 0L;
    }
}
