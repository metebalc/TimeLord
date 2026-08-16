package com.timelord.common.model;

import java.util.Objects;
import java.util.UUID;

/** Damage captured during stopped time and resolved after time resumes. */
public record PendingHit(
        UUID hitId,
        UUID targetId,
        UUID attackerId,
        float damage,
        TemporalPosition impactPosition,
        TemporalPosition attackDirection
) {
    public PendingHit {
        Objects.requireNonNull(hitId, "hitId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(attackerId, "attackerId");
        Objects.requireNonNull(impactPosition, "impactPosition");
        Objects.requireNonNull(attackDirection, "attackDirection");
    }
}
