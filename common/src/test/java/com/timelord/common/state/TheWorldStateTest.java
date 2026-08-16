package com.timelord.common.state;

import com.timelord.common.model.PendingHit;
import com.timelord.common.model.TemporalPosition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TheWorldStateTest {
    @Test
    void tracksMultipleUsersAndReportsTheGlobalResumeTransition() {
        TheWorldState state = new TheWorldState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(state.activate(first, 10));
        assertFalse(state.activate(second, 10));
        assertEquals(TheWorldState.DeactivationResult.STILL_STOPPED, state.deactivate(first));
        assertTrue(state.isTimeStopped());
        assertEquals(TheWorldState.DeactivationResult.TIME_RESUMED, state.deactivate(second));
        assertFalse(state.isTimeStopped());
    }

    @Test
    void expiresUsersInActivationOrder() {
        TheWorldState state = new TheWorldState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        state.activate(first, 1);
        state.activate(second, 1);

        assertEquals(List.of(first, second), state.tickDurations());
        assertFalse(state.isTimeStopped());
    }

    @Test
    void drainsPendingHitsWithoutRetainingMinecraftObjects() {
        TheWorldState state = new TheWorldState();
        PendingHit hit = new PendingHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                4.0F,
                TemporalPosition.ZERO,
                new TemporalPosition(0.0D, 0.0D, 1.0D)
        );

        state.storeHit(hit);

        assertEquals(List.of(hit), state.drainPendingHits());
        assertTrue(state.pendingHits().isEmpty());
    }

    @Test
    void delayedHitBatchResolvesAfterTwoDecrementTicks() {
        PendingHit hit = new PendingHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                4.0F,
                TemporalPosition.ZERO,
                new TemporalPosition(0.0D, 0.0D, 1.0D)
        );
        PendingHitBatch batch = new PendingHitBatch(List.of(hit), 2);

        assertFalse(batch.ready());
        batch = batch.tick();
        assertFalse(batch.ready());
        batch = batch.tick();
        assertTrue(batch.ready());
    }
}
