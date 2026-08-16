package com.timelord.client.state;

import com.timelord.future.ThreatType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientFutureSightState {
    private static final Map<Integer, ThreatType> THREATS = new LinkedHashMap<>();

    private ClientFutureSightState() {}

    public static void setThreats(Map<Integer, ThreatType> threats) {
        THREATS.clear();
        THREATS.putAll(threats);
    }

    public static Map<Integer, ThreatType> getThreats() {
        return Collections.unmodifiableMap(THREATS);
    }

    public static void clear() {
        THREATS.clear();
    }
}
