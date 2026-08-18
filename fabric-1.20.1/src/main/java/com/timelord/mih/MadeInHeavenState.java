package com.timelord.mih;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server-owned global Made in Heaven lifecycle. Minecraft integration is kept at its boundary.
 */
public final class MadeInHeavenState {
    private final Set<UUID> activeUsers = new LinkedHashSet<>();
    private final Map<UUID, UniverseSnapshot> snapshots = new LinkedHashMap<>();

    private Phase phase = Phase.INACTIVE;
    private long generationId;
    private int elapsedActiveTicks;
    private int collapseElapsedTicks;
    private double collapseStartPhysicalScale = 1.0D;
    private double collapseStartVisualFactor = 1.0D;
    private boolean resetRequired;

    public ActivationResult activate(UUID playerId, Collection<UniverseSnapshot> initialSnapshots) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(initialSnapshots, "initialSnapshots");

        if (phase == Phase.RESETTING)
            return ActivationResult.REJECTED_RESETTING;

        boolean startedGeneration = phase == Phase.INACTIVE;
        boolean resumedCollapse = phase == Phase.COLLAPSING;

        if (startedGeneration) {
            generationId++;
            elapsedActiveTicks = 0;
            snapshots.clear();
            phase = Phase.BUILDUP;
            for (UniverseSnapshot snapshot : initialSnapshots)
                enrollSnapshot(snapshot);
        }

        if (startedGeneration || resumedCollapse) {
            phase = Phase.BUILDUP;
            collapseElapsedTicks = 0;
            collapseStartPhysicalScale = 1.0D;
            collapseStartVisualFactor = 1.0D;
        }

        boolean added = activeUsers.add(playerId);
        if (startedGeneration)
            return ActivationResult.STARTED_GENERATION;
        if (resumedCollapse)
            return ActivationResult.RESUMED_BUILDUP;
        return added ? ActivationResult.JOINED_BUILDUP : ActivationResult.ALREADY_ACTIVE;
    }

    public void enrollSnapshot(UniverseSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (phase == Phase.INACTIVE)
            throw new IllegalStateException("Cannot enroll a snapshot without an active generation");
        snapshots.putIfAbsent(snapshot.playerId(), snapshot);
    }

    public DeactivationResult deactivate(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (!activeUsers.remove(playerId))
            return DeactivationResult.NOT_ACTIVE;

        if (activeUsers.isEmpty() && phase == Phase.BUILDUP) {
            beginCollapse();
            return DeactivationResult.STARTED_COLLAPSE;
        }
        return DeactivationResult.LEFT_BUILDUP;
    }

    public Set<UUID> removeInvalidUsers(Predicate<UUID> isInvalid) {
        Objects.requireNonNull(isInvalid, "isInvalid");
        Set<UUID> removed = new LinkedHashSet<>();
        activeUsers.removeIf(playerId -> {
            if (!isInvalid.test(playerId))
                return false;
            removed.add(playerId);
            return true;
        });

        if (!removed.isEmpty() && activeUsers.isEmpty() && phase == Phase.BUILDUP)
            beginCollapse();
        return Collections.unmodifiableSet(removed);
    }

    public TickResult tick(boolean theWorldActive) {
        if (phase == Phase.INACTIVE)
            return TickResult.INACTIVE;
        if (phase == Phase.RESETTING)
            return TickResult.RESET_REQUIRED;
        if (theWorldActive)
            return TickResult.PAUSED_BY_THE_WORLD;

        if (phase == Phase.COLLAPSING) {
            collapseElapsedTicks++;
            if (collapseElapsedTicks >= MadeInHeavenCurves.COLLAPSE_TICKS) {
                clearGeneration();
                return TickResult.COLLAPSE_COMPLETED;
            }
            return TickResult.COLLAPSE_ADVANCED;
        }

        if (activeUsers.isEmpty()) {
            beginCollapse();
            return TickResult.COLLAPSE_ADVANCED;
        }

        elapsedActiveTicks++;
        if (elapsedActiveTicks >= MadeInHeavenCurves.BUILDUP_TICKS) {
            elapsedActiveTicks = MadeInHeavenCurves.BUILDUP_TICKS;
            phase = Phase.RESETTING;
            resetRequired = true;
            return TickResult.RESET_REQUIRED;
        }
        return TickResult.BUILDUP_ADVANCED;
    }

    public void completeReset() {
        if (phase != Phase.RESETTING || !resetRequired)
            throw new IllegalStateException("No Universe Reset is pending");
        clearGeneration();
    }

    public void clear() {
        clearGeneration();
    }

    public MadeInHeavenSyncState synchronizedState(int serverTick, boolean theWorldActive) {
        return new MadeInHeavenSyncState(
                generationId,
                phase,
                elapsedActiveTicks,
                collapseElapsedTicks,
                serverTick,
                theWorldActive,
                activeUsers
        );
    }

    public Phase phase() {
        return phase;
    }

    public long generationId() {
        return generationId;
    }

    public int elapsedActiveTicks() {
        return elapsedActiveTicks;
    }

    public int collapseElapsedTicks() {
        return collapseElapsedTicks;
    }

    public double progress() {
        return elapsedActiveTicks / (double) MadeInHeavenCurves.BUILDUP_TICKS;
    }

    public boolean isActiveUser(UUID playerId) {
        return activeUsers.contains(playerId);
    }

    public Set<UUID> activeUsers() {
        return Collections.unmodifiableSet(activeUsers);
    }

    public Map<UUID, UniverseSnapshot> snapshots() {
        return Collections.unmodifiableMap(snapshots);
    }

    public double physicalScaleForNormalPlayers() {
        return switch (phase) {
            case INACTIVE -> 1.0D;
            case BUILDUP, RESETTING -> MadeInHeavenCurves.physicalScale(elapsedActiveTicks);
            case COLLAPSING -> MadeInHeavenCurves.collapsePhysicalScale(
                    collapseStartPhysicalScale,
                    collapseElapsedTicks / (double) MadeInHeavenCurves.COLLAPSE_TICKS
            );
        };
    }

    public double visualWorldFactorForNormalPlayers() {
        return switch (phase) {
            case INACTIVE -> 1.0D;
            case BUILDUP, RESETTING -> MadeInHeavenCurves.visualWorldFactor(elapsedActiveTicks);
            case COLLAPSING -> MadeInHeavenCurves.collapseVisualFactor(
                    collapseStartVisualFactor,
                    collapseElapsedTicks / (double) MadeInHeavenCurves.COLLAPSE_TICKS
            );
        };
    }

    private void beginCollapse() {
        if (phase != Phase.BUILDUP)
            return;
        collapseStartPhysicalScale = MadeInHeavenCurves.physicalScale(elapsedActiveTicks);
        collapseStartVisualFactor = MadeInHeavenCurves.visualWorldFactor(elapsedActiveTicks);
        collapseElapsedTicks = 0;
        phase = Phase.COLLAPSING;
    }

    private void clearGeneration() {
        activeUsers.clear();
        snapshots.clear();
        phase = Phase.INACTIVE;
        elapsedActiveTicks = 0;
        collapseElapsedTicks = 0;
        collapseStartPhysicalScale = 1.0D;
        collapseStartVisualFactor = 1.0D;
        resetRequired = false;
    }

    public enum Phase {
        INACTIVE,
        BUILDUP,
        COLLAPSING,
        RESETTING
    }

    public enum ActivationResult {
        STARTED_GENERATION,
        JOINED_BUILDUP,
        RESUMED_BUILDUP,
        ALREADY_ACTIVE,
        REJECTED_RESETTING
    }

    public enum DeactivationResult {
        LEFT_BUILDUP,
        STARTED_COLLAPSE,
        NOT_ACTIVE
    }

    public enum TickResult {
        INACTIVE,
        BUILDUP_ADVANCED,
        COLLAPSE_ADVANCED,
        COLLAPSE_COMPLETED,
        PAUSED_BY_THE_WORLD,
        RESET_REQUIRED
    }
}
