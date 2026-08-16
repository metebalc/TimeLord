package com.timelord.common.state;

import com.timelord.common.model.PendingHit;

import java.util.List;
import java.util.Objects;

/** Immutable delayed-resolution batch for hits released when stopped time ends. */
public record PendingHitBatch(List<PendingHit> hits, int ticksRemaining) {
    public PendingHitBatch {
        Objects.requireNonNull(hits, "hits");
        hits = List.copyOf(hits);
        if (ticksRemaining < 0)
            throw new IllegalArgumentException("ticksRemaining must not be negative");
    }

    public PendingHitBatch tick() {
        return ticksRemaining == 0 ? this : new PendingHitBatch(hits, ticksRemaining - 1);
    }

    public boolean ready() {
        return ticksRemaining == 0;
    }
}
