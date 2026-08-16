package com.timelord.common.logic;

import com.timelord.common.ability.AbilityId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownTrackerTest {
    @Test
    void tracksCooldownsAgainstAnInjectedGameTick() {
        CooldownTracker tracker = new CooldownTracker();
        UUID playerId = UUID.randomUUID();
        long startTick = 1_000L;

        tracker.start(playerId, AbilityId.TIME_REWIND, startTick);

        assertTrue(tracker.isOnCooldown(playerId, AbilityId.TIME_REWIND, startTick));
        assertEquals(300, tracker.remainingTicks(playerId, AbilityId.TIME_REWIND, startTick));
        assertEquals(100, tracker.remainingTicks(playerId, AbilityId.TIME_REWIND, startTick + 200));
        assertFalse(tracker.isOnCooldown(playerId, AbilityId.TIME_REWIND, startTick + 300));
    }
}
