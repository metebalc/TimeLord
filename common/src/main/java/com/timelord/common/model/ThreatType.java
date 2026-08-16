package com.timelord.common.model;

/** Stable threat categories and their existing wire IDs. */
public enum ThreatType {
    DANGEROUS_PROJECTILE(0),
    HOSTILE_MOB(1);

    private final int networkId;

    ThreatType(int networkId) {
        this.networkId = networkId;
    }

    public int networkId() {
        return networkId;
    }

    public static ThreatType fromNetworkId(int networkId) {
        for (ThreatType type : values()) {
            if (type.networkId == networkId)
                return type;
        }

        return null;
    }
}
