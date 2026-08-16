package com.timelord.client.state;

import com.timelord.common.model.TemporalPosition;
import com.timelord.common.state.TimeRewindEffectState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientTimeRewindState {
    private static final List<TimeRewindEffectState> EFFECTS = new ArrayList<>();

    private ClientTimeRewindState() {}

    public static void start(
            UUID playerId,
            TemporalPosition origin,
            TemporalPosition destination,
            int durationTicks
    ) {
        EFFECTS.add(TimeRewindEffectState.start(playerId, origin, destination, durationTicks));
    }

    public static void tick() {
        var iterator = EFFECTS.listIterator();

        while (iterator.hasNext()) {
            TimeRewindEffectState effect = iterator.next().tick();

            if (effect.expired()) {
                iterator.remove();
            } else {
                iterator.set(effect);
            }
        }
    }

    public static List<TimeRewindEffectState> getEffects() {
        return List.copyOf(EFFECTS);
    }

    public static void clear() {
        EFFECTS.clear();
    }

}
