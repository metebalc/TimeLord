package com.timelord.common.network.message;

import com.timelord.common.model.TemporalPosition;

import java.util.UUID;

public final class TimeFieldMessages {
    private TimeFieldMessages() {}

    public record Started(
            UUID ownerId,
            TemporalPosition center,
            double radius,
            int durationTicks
    ) {}

    public record Removed(UUID ownerId) {}
}
