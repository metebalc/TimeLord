package com.timelord.mih;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FractionalTickAccumulatorTest {
    @Test
    void eightPercentProducesEightPermitsPerHundredCalls() {
        FractionalTickAccumulator accumulator = new FractionalTickAccumulator();
        UUID entityId = UUID.randomUUID();
        int permits = 0;

        for (int tick = 0; tick < 100; tick++) {
            if (accumulator.shouldStep(entityId, 0.08D))
                permits++;
        }

        assertEquals(8, permits);
    }

    @Test
    void stoppedAndNormalScalesAreHandledExplicitly() {
        FractionalTickAccumulator accumulator = new FractionalTickAccumulator();
        UUID entityId = UUID.randomUUID();

        assertFalse(accumulator.shouldStep(entityId, 0.0D));
        assertTrue(accumulator.shouldStep(entityId, 1.0D));
        assertTrue(accumulator.shouldStep(entityId, 4.0D));
    }

    @Test
    void entitiesKeepIndependentRemainders() {
        FractionalTickAccumulator accumulator = new FractionalTickAccumulator();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertFalse(accumulator.shouldStep(first, 0.6D));
        assertFalse(accumulator.shouldStep(second, 0.4D));
        assertTrue(accumulator.shouldStep(first, 0.6D));
        assertFalse(accumulator.shouldStep(second, 0.4D));
    }

    @Test
    void removingAPlayerDiscardsLifecycleRemainder() {
        FractionalTickAccumulator accumulator = new FractionalTickAccumulator();
        UUID playerId = UUID.randomUUID();

        assertFalse(accumulator.shouldStep(playerId, 0.75D));
        accumulator.remove(playerId);
        assertFalse(accumulator.shouldStep(playerId, 0.25D));
    }

    @Test
    void retainAllDropsDisconnectedEntitiesWithoutAffectingOnlineOnes() {
        FractionalTickAccumulator accumulator = new FractionalTickAccumulator();
        UUID online = UUID.randomUUID();
        UUID disconnected = UUID.randomUUID();

        assertFalse(accumulator.shouldStep(online, 0.6D));
        assertFalse(accumulator.shouldStep(disconnected, 0.75D));
        accumulator.retainAll(Set.of(online));

        assertTrue(accumulator.shouldStep(online, 0.4D));
        assertFalse(accumulator.shouldStep(disconnected, 0.25D));
    }
}
