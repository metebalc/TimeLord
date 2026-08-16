package com.timelord.common.network.message;

import com.timelord.common.model.TemporalPosition;

import java.util.UUID;

public final class TimeRewindMessages {
    private TimeRewindMessages() {}

    public record Effect(
            UUID playerId,
            TemporalPosition origin,
            TemporalPosition destination,
            int durationTicks
    ) {}
}
