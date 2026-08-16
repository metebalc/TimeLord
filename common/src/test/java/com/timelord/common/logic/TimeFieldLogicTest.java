package com.timelord.common.logic;

import com.timelord.common.model.TemporalPosition;
import com.timelord.common.state.SlowFieldState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeFieldLogicTest {
    @Test
    void choosesTheLargestIntervalFromOverlappingFields() {
        UUID entityId = UUID.randomUUID();
        List<SlowFieldState> fields = List.of(
                field(0.5F, null),
                field(0.1F, null)
        );

        int interval = TimeFieldLogic.tickInterval(
                fields,
                "minecraft:overworld",
                entityId,
                TemporalPosition.ZERO
        );

        assertEquals(10, interval);
        assertTrue(TimeFieldLogic.shouldTick(20L, interval));
        assertFalse(TimeFieldLogic.shouldTick(21L, interval));
    }

    @Test
    void excludesTheFieldOwner() {
        UUID ownerId = UUID.randomUUID();

        assertEquals(1, TimeFieldLogic.tickInterval(
                List.of(field(0.1F, ownerId)),
                "minecraft:overworld",
                ownerId,
                TemporalPosition.ZERO
        ));
    }

    private static SlowFieldState field(float scale, UUID excludedId) {
        return new SlowFieldState(
                "minecraft:overworld",
                TemporalPosition.ZERO,
                16.0D,
                scale,
                200,
                excludedId
        );
    }
}
