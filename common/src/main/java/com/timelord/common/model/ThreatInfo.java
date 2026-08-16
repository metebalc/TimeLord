package com.timelord.common.model;

import java.util.Objects;

/** A client-resolvable threat result. Entity IDs remain part of the existing protocol. */
public record ThreatInfo(int entityId, ThreatType type) {
    public ThreatInfo {
        Objects.requireNonNull(type, "type");
    }
}
