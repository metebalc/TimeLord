package com.timelord.client.state;

import com.timelord.common.model.ThreatInfo;
import com.timelord.common.model.ThreatType;
import com.timelord.common.state.FutureSightThreatState;

import java.util.Collection;
import java.util.Map;

public final class ClientFutureSightState {
    private static final FutureSightThreatState STATE = new FutureSightThreatState();

    private ClientFutureSightState() {}

    public static void setThreats(Collection<ThreatInfo> threats) {
        STATE.replace(threats);
    }

    public static Map<Integer, ThreatType> getThreats() {
        return STATE.threats();
    }

    public static void clear() {
        STATE.clear();
    }
}
