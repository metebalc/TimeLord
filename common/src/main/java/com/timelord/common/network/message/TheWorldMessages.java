package com.timelord.common.network.message;

import com.timelord.common.model.TemporalPosition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TheWorldMessages {
    private TheWorldMessages() {}

    public record State(Map<UUID, Integer> activeDurations, int maxDurationTicks) {
        public State {
            activeDurations = Collections.unmodifiableMap(new LinkedHashMap<>(activeDurations));
        }
    }

    public record Activation(UUID activatorId, boolean globalTransition) {}

    public record StoredHit(
            UUID hitId,
            UUID targetId,
            UUID attackerId,
            TemporalPosition impactPosition,
            TemporalPosition attackDirection
    ) {}

    public record ResolveHit(UUID hitId, int sequenceIndex, int totalHits) {}
}
