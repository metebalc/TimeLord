package com.timelord.common.model;

import java.util.Objects;

/** Server-captured player state that contains no Minecraft objects. */
public record TemporalSnapshot(
        String dimensionId,
        TemporalPosition position,
        float yaw,
        float pitch,
        TemporalPosition velocity,
        float health
) {
    public TemporalSnapshot {
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(velocity, "velocity");
    }
}
