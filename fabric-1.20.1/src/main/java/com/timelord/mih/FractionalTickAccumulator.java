package com.timelord.mih;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Converts a fractional temporal scale into deterministic whole-tick permits. */
public final class FractionalTickAccumulator {
    private final Map<UUID, Double> accumulatedTicks = new HashMap<>();

    public boolean shouldStep(UUID entityId, double temporalScale) {
        if (!Double.isFinite(temporalScale) || temporalScale <= 0.0D) {
            accumulatedTicks.remove(entityId);
            return false;
        }
        if (temporalScale >= 1.0D) {
            accumulatedTicks.remove(entityId);
            return true;
        }

        double accumulated = accumulatedTicks.getOrDefault(entityId, 0.0D) + temporalScale;
        if (accumulated + 1.0E-12D < 1.0D) {
            accumulatedTicks.put(entityId, accumulated);
            return false;
        }

        accumulatedTicks.put(entityId, accumulated - 1.0D);
        return true;
    }

    public void remove(UUID entityId) {
        accumulatedTicks.remove(entityId);
    }

    public void clear() {
        accumulatedTicks.clear();
    }

    public void retainAll(Set<UUID> entityIds) {
        accumulatedTicks.keySet().retainAll(entityIds);
    }
}
