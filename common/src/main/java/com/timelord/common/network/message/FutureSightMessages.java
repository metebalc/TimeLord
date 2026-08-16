package com.timelord.common.network.message;

import com.timelord.common.model.ThreatInfo;

import java.util.List;

public final class FutureSightMessages {
    private FutureSightMessages() {}

    public record Threats(List<ThreatInfo> threats) {
        public Threats {
            threats = List.copyOf(threats);
        }
    }
}
