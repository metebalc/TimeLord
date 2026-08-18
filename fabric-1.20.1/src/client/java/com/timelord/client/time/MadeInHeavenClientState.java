package com.timelord.client.time;

import com.timelord.mih.MadeInHeavenCurves;
import com.timelord.mih.MadeInHeavenCinematicTimeline;
import com.timelord.mih.MadeInHeavenState;
import com.timelord.mih.MadeInHeavenSyncState;

import java.util.Set;
import java.util.UUID;

/** Client mirror that predicts between periodic authoritative corrections. */
public final class MadeInHeavenClientState {
    private static MadeInHeavenSyncState state = MadeInHeavenSyncState.inactive(0);
    private static final MadeInHeavenCinematicTimeline CINEMATIC =
            new MadeInHeavenCinematicTimeline();

    private MadeInHeavenClientState() {}

    public static void apply(MadeInHeavenSyncState synchronizedState) {
        CINEMATIC.observe(synchronizedState.generationId(), synchronizedState.phase());
        state = synchronizedState;
    }

    public static void tick(boolean theWorldActive) {
        CINEMATIC.tick();
        MadeInHeavenState.Phase phase = state.phase();
        if (phase == MadeInHeavenState.Phase.INACTIVE
                || phase == MadeInHeavenState.Phase.RESETTING
                || theWorldActive)
            return;

        int elapsed = state.elapsedActiveTicks();
        int collapseElapsed = state.collapseElapsedTicks();
        if (phase == MadeInHeavenState.Phase.BUILDUP) {
            elapsed = Math.min(MadeInHeavenCurves.BUILDUP_TICKS, elapsed + 1);
            if (elapsed >= MadeInHeavenCurves.BUILDUP_TICKS)
                phase = MadeInHeavenState.Phase.RESETTING;
        } else if (phase == MadeInHeavenState.Phase.COLLAPSING) {
            collapseElapsed = Math.min(MadeInHeavenCurves.COLLAPSE_TICKS, collapseElapsed + 1);
            if (collapseElapsed >= MadeInHeavenCurves.COLLAPSE_TICKS) {
                state = MadeInHeavenSyncState.inactive(state.serverTick() + 1);
                return;
            }
        }

        state = new MadeInHeavenSyncState(
                state.generationId(),
                phase,
                elapsed,
                collapseElapsed,
                state.serverTick() + 1,
                false,
                state.activeUsers()
        );
    }

    public static MadeInHeavenState.Phase phase() {
        return state.phase();
    }

    public static int elapsedActiveTicks() {
        return state.elapsedActiveTicks();
    }

    public static Set<UUID> activeUsers() {
        return state.activeUsers();
    }

    public static boolean isActiveUser(UUID playerId) {
        return state.activeUsers().contains(playerId);
    }

    public static double theWorldResistanceFor(UUID playerId) {
        if (!isActiveUser(playerId))
            return 0.0D;
        return MadeInHeavenCurves.theWorldResistance(state.elapsedActiveTicks());
    }

    /** Client-only perceptual frame used for observer-relative rendering. */
    public static double perceptualScaleFor(UUID playerId) {
        if (state.phase() == MadeInHeavenState.Phase.INACTIVE || isActiveUser(playerId))
            return 1.0D;
        if (state.phase() == MadeInHeavenState.Phase.COLLAPSING) {
            return MadeInHeavenCurves.collapsePhysicalScale(
                    MadeInHeavenCurves.physicalScale(state.elapsedActiveTicks()),
                    state.collapseElapsedTicks() / (double) MadeInHeavenCurves.COLLAPSE_TICKS
            );
        }
        return MadeInHeavenCurves.physicalScale(state.elapsedActiveTicks());
    }

    public static double visualWorldFactorFor(UUID viewerId) {
        if (state.phase() == MadeInHeavenState.Phase.INACTIVE || isActiveUser(viewerId))
            return 1.0D;
        if (state.phase() == MadeInHeavenState.Phase.COLLAPSING) {
            return MadeInHeavenCurves.collapseVisualFactor(
                    MadeInHeavenCurves.visualWorldFactor(state.elapsedActiveTicks()),
                    state.collapseElapsedTicks() / (double) MadeInHeavenCurves.COLLAPSE_TICKS
            );
        }
        return MadeInHeavenCurves.visualWorldFactor(state.elapsedActiveTicks());
    }

    public static double visualWorldOffsetTicksFor(UUID viewerId) {
        if (state.phase() == MadeInHeavenState.Phase.INACTIVE || isActiveUser(viewerId))
            return 0.0D;

        double buildupOffset = MadeInHeavenCurves.visualWorldOffsetTicks(
                state.elapsedActiveTicks());
        if (state.phase() != MadeInHeavenState.Phase.COLLAPSING)
            return buildupOffset;

        return MadeInHeavenCurves.collapseVisualWorldOffset(
                buildupOffset,
                state.collapseElapsedTicks() / (double) MadeInHeavenCurves.COLLAPSE_TICKS
        );
    }

    public static MadeInHeavenSyncState synchronizedState() {
        return state;
    }

    public static float cinematicAlpha(UUID viewerId, float tickDelta) {
        float prelude = state.phase() == MadeInHeavenState.Phase.BUILDUP
                || state.phase() == MadeInHeavenState.Phase.RESETTING
                ? MadeInHeavenCinematicTimeline.preludeAlpha(
                        state.elapsedActiveTicks(), isActiveUser(viewerId))
                : 0.0F;
        return Math.max(prelude, CINEMATIC.flashAlpha(tickDelta));
    }

    public static void clear() {
        state = MadeInHeavenSyncState.inactive(0);
        CINEMATIC.clear();
    }
}
