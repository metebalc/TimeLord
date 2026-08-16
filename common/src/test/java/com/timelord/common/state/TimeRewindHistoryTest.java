package com.timelord.common.state;

import com.timelord.common.model.TemporalPosition;
import com.timelord.common.model.TemporalSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeRewindHistoryTest {
    @Test
    void keepsABoundedHistoryAndIndexesFromTheNewestSnapshot() {
        TimeRewindHistory history = new TimeRewindHistory(3);
        UUID playerId = UUID.randomUUID();

        history.record(playerId, snapshot("minecraft:overworld", 1.0D));
        history.record(playerId, snapshot("minecraft:overworld", 2.0D));
        history.record(playerId, snapshot("minecraft:overworld", 3.0D));
        history.record(playerId, snapshot("minecraft:overworld", 4.0D));

        assertEquals(4.0D, history.get(playerId, "minecraft:overworld", 0).orElseThrow().position().x());
        assertEquals(2.0D, history.get(playerId, "minecraft:overworld", 2).orElseThrow().position().x());
        assertTrue(history.get(playerId, "minecraft:overworld", 3).isEmpty());
    }

    @Test
    void resetsHistoryWhenTheDimensionChanges() {
        TimeRewindHistory history = new TimeRewindHistory(80);
        UUID playerId = UUID.randomUUID();

        history.record(playerId, snapshot("minecraft:overworld", 1.0D));
        history.record(playerId, snapshot("minecraft:the_nether", 2.0D));

        assertTrue(history.get(playerId, "minecraft:overworld", 0).isEmpty());
        assertEquals(2.0D, history.get(playerId, "minecraft:the_nether", 0).orElseThrow().position().x());
    }

    private static TemporalSnapshot snapshot(String dimensionId, double x) {
        return new TemporalSnapshot(
                dimensionId,
                new TemporalPosition(x, 64.0D, 0.0D),
                0.0F,
                0.0F,
                TemporalPosition.ZERO,
                20.0F
        );
    }
}
