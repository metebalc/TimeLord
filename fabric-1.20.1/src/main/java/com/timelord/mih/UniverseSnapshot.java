package com.timelord.mih;

import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight, duplication-safe state captured for a future Universe Reset.
 */
public record UniverseSnapshot(
        UUID playerId,
        String dimensionId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double velocityX,
        double velocityY,
        double velocityZ,
        float health
) {
    public UniverseSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        if (dimensionId.isBlank())
            throw new IllegalArgumentException("dimensionId cannot be blank");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Snapshot position must be finite");
        if (!Double.isFinite(velocityX) || !Double.isFinite(velocityY) || !Double.isFinite(velocityZ))
            throw new IllegalArgumentException("Snapshot velocity must be finite");
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(health))
            throw new IllegalArgumentException("Snapshot orientation and health must be finite");
    }
}
