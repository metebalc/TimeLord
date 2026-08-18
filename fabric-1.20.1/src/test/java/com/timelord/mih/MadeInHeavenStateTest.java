package com.timelord.mih;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MadeInHeavenStateTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void additionalUsersJoinTheExistingGlobalTimer() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertEquals(MadeInHeavenState.ActivationResult.STARTED_GENERATION,
                state.activate(first, List.of(snapshot(first))));
        tick(state, 500, false);

        assertEquals(MadeInHeavenState.ActivationResult.JOINED_BUILDUP,
                state.activate(second, List.of()));
        assertEquals(500, state.elapsedActiveTicks());
        assertEquals(2, state.activeUsers().size());
    }

    @Test
    void theWorldPausesBuildupAndCollapse() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID user = UUID.randomUUID();
        state.activate(user, List.of(snapshot(user)));
        tick(state, 884, false);

        tick(state, 100, true);
        assertEquals(884, state.elapsedActiveTicks());

        state.deactivate(user);
        tick(state, 20, true);
        assertEquals(0, state.collapseElapsedTicks());
    }

    @Test
    void buildupResumesFromTheExactTickAfterTheWorldEnds() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID firstMihUser = UUID.randomUUID();
        UUID secondMihUser = UUID.randomUUID();
        state.activate(firstMihUser, List.of(snapshot(firstMihUser)));
        tick(state, 884, false);

        state.activate(secondMihUser, List.of());
        tick(state, 200, true);
        assertEquals(884, state.elapsedActiveTicks());
        assertEquals(2, state.activeUsers().size());

        assertEquals(MadeInHeavenState.TickResult.BUILDUP_ADVANCED, state.tick(false));
        assertEquals(885, state.elapsedActiveTicks());
    }

    @Test
    void lastUserStartsSmoothCollapseAndReactivationResumesProgress() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID user = UUID.randomUUID();
        state.activate(user, List.of(snapshot(user)));
        tick(state, 800, false);
        double startScale = state.physicalScaleForNormalPlayers();

        assertEquals(MadeInHeavenState.DeactivationResult.STARTED_COLLAPSE, state.deactivate(user));
        assertEquals(MadeInHeavenState.Phase.COLLAPSING, state.phase());
        tick(state, 40, false);
        assertTrue(state.physicalScaleForNormalPlayers() > startScale);
        assertTrue(state.physicalScaleForNormalPlayers() < 1.0D);

        assertEquals(MadeInHeavenState.ActivationResult.RESUMED_BUILDUP,
                state.activate(user, List.of()));
        assertEquals(800, state.elapsedActiveTicks());
        assertEquals(startScale, state.physicalScaleForNormalPlayers(), EPSILON);
    }

    @Test
    void collapseCompletesAfterExactlyEightyTicks() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID user = UUID.randomUUID();
        state.activate(user, List.of(snapshot(user)));
        tick(state, 600, false);
        state.deactivate(user);

        tick(state, MadeInHeavenCurves.COLLAPSE_TICKS - 1, false);
        assertEquals(MadeInHeavenState.Phase.COLLAPSING, state.phase());
        assertEquals(MadeInHeavenState.TickResult.COLLAPSE_COMPLETED, state.tick(false));
        assertEquals(MadeInHeavenState.Phase.INACTIVE, state.phase());
        assertEquals(1.0D, state.physicalScaleForNormalPlayers(), EPSILON);
    }

    @Test
    void resetBecomesRequiredOnceAtTickTwelveHundred() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID user = UUID.randomUUID();
        state.activate(user, List.of(snapshot(user)));

        tick(state, MadeInHeavenCurves.BUILDUP_TICKS - 1, false);
        assertEquals(MadeInHeavenState.Phase.BUILDUP, state.phase());
        assertEquals(MadeInHeavenState.TickResult.RESET_REQUIRED, state.tick(false));
        assertEquals(MadeInHeavenState.Phase.RESETTING, state.phase());
        assertEquals(MadeInHeavenCurves.BUILDUP_TICKS, state.elapsedActiveTicks());
        assertEquals(MadeInHeavenState.Phase.RESETTING,
                state.synchronizedState(1200, false).phase());
        assertEquals(MadeInHeavenState.TickResult.RESET_REQUIRED, state.tick(false));

        state.completeReset();
        assertEquals(MadeInHeavenState.Phase.INACTIVE, state.phase());
        assertThrows(IllegalStateException.class, state::completeReset);
    }

    @Test
    void snapshotsAreFirstCaptureOnlyAndNeverContainInventoryState() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID user = UUID.randomUUID();
        UniverseSnapshot first = snapshot(user);
        UniverseSnapshot replacement = new UniverseSnapshot(
                user, "minecraft:the_nether", 99.0D, 80.0D, 99.0D,
                90.0F, 0.0F, 0.0D, 0.0D, 0.0D, 5.0F
        );

        state.activate(user, List.of(first));
        state.enrollSnapshot(replacement);
        assertEquals(first, state.snapshots().get(user));
        assertEquals(11, UniverseSnapshot.class.getRecordComponents().length);
    }

    @Test
    void synchronizedStateIsCompactAndDefensivelyCopiesMembership() {
        MadeInHeavenState state = new MadeInHeavenState();
        UUID user = UUID.randomUUID();
        state.activate(user, List.of(snapshot(user)));
        tick(state, 321, false);

        MadeInHeavenSyncState synchronizedState = state.synchronizedState(900, false);
        assertEquals(321, synchronizedState.elapsedActiveTicks());
        assertEquals(Set.of(user), synchronizedState.activeUsers());
        assertThrows(UnsupportedOperationException.class,
                () -> synchronizedState.activeUsers().add(UUID.randomUUID()));
    }

    private static void tick(MadeInHeavenState state, int count, boolean theWorldActive) {
        for (int i = 0; i < count; i++)
            state.tick(theWorldActive);
    }

    private static UniverseSnapshot snapshot(UUID playerId) {
        return new UniverseSnapshot(
                playerId, "minecraft:overworld", 1.0D, 64.0D, 2.0D,
                0.0F, 0.0F, 0.0D, 0.0D, 0.0D, 20.0F
        );
    }
}
