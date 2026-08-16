package com.timelord.common.state;

import com.timelord.common.model.ThreatInfo;
import com.timelord.common.model.ThreatType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Ordered render-neutral threat state for Future Sight. */
public final class FutureSightThreatState {
    private final LinkedHashMap<Integer, ThreatType> threats = new LinkedHashMap<>();

    public void replace(Collection<ThreatInfo> newThreats) {
        Objects.requireNonNull(newThreats, "newThreats");
        threats.clear();

        for (ThreatInfo threat : newThreats) {
            Objects.requireNonNull(threat, "threat");
            threats.put(threat.entityId(), threat.type());
        }
    }

    public Map<Integer, ThreatType> threats() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(threats));
    }

    public void clear() {
        threats.clear();
    }
}
