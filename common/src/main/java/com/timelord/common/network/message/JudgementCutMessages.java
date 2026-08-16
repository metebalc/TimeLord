package com.timelord.common.network.message;

import com.timelord.common.model.TemporalPosition;

public final class JudgementCutMessages {
    private JudgementCutMessages() {}

    public record Start(TemporalPosition center) {}

    public record Release(double radius, long seed, int slashCount) {}

    public record Clear() {}

    public record Monochrome(boolean active) {}
}
