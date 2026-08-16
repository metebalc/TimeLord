package com.timelord.future;

public enum ThreatType {
    HOSTILE_MOB(0),
    DANGEROUS_PROJECTILE(1);

    private final int networkId;

    ThreatType(int networkId) {
        this.networkId = networkId;
    }

    public int networkId() {
        return networkId;
    }

    public static ThreatType fromNetworkId(int id) {
        for (ThreatType type : values()) {
            if (type.networkId == id)
                return type;
        }

        return null;
    }
}
