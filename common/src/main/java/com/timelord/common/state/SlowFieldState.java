package com.timelord.common.state;

import com.timelord.common.model.TemporalPosition;

import java.util.Objects;
import java.util.UUID;

/** Version-neutral state for a localized time-slowing field. */
public record SlowFieldState(
        String dimensionId,
        TemporalPosition center,
        double radius,
        float scale,
        int remainingTicks,
        UUID excludedEntityId
) {
    public SlowFieldState {
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(center, "center");
        if (radius < 0.0D)
            throw new IllegalArgumentException("radius must not be negative");
        if (scale <= 0.0F || scale > 1.0F)
            throw new IllegalArgumentException("scale must be in the range (0, 1]");
        if (remainingTicks < 0)
            throw new IllegalArgumentException("remainingTicks must not be negative");
    }

    public SlowFieldState withRemainingTicks(int ticks) {
        return new SlowFieldState(dimensionId, center, radius, scale, ticks, excludedEntityId);
    }

    public boolean contains(String entityDimensionId, UUID entityId, TemporalPosition entityPosition) {
        return dimensionId.equals(entityDimensionId)
                && !Objects.equals(excludedEntityId, entityId)
                && center.squaredDistanceTo(entityPosition) <= radius * radius;
    }

    public int tickInterval() {
        return Math.max(1, Math.round(1.0F / scale));
    }
}
