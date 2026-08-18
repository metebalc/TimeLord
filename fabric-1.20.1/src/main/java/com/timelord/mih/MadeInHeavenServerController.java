package com.timelord.mih;

import com.timelord.ModSounds;
import com.timelord.ability.TheWorldAbility;
import com.timelord.network.MadeInHeavenNetworking;
import com.timelord.time.TimeController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Owns the global 1.20.1 Made in Heaven state on the logical server. */
public final class MadeInHeavenServerController {
    private static final int CORRECTION_INTERVAL_TICKS = 40;
    private static final int ENDING_SOUND_START_TICK =
            MadeInHeavenCurves.BUILDUP_TICKS - 30 * 20;
    private static final MadeInHeavenState STATE = new MadeInHeavenState();

    private MadeInHeavenServerController() {}

    public static boolean activate(ServerPlayerEntity player) {
        if (!isEligible(player) || STATE.phase() == MadeInHeavenState.Phase.RESETTING)
            return false;

        List<UniverseSnapshot> initialSnapshots = STATE.phase() == MadeInHeavenState.Phase.INACTIVE
                ? captureInitialSnapshots(player.getServer())
                : List.of();
        MadeInHeavenState.ActivationResult result = STATE.activate(
                player.getUuid(),
                initialSnapshots
        );
        if (result == MadeInHeavenState.ActivationResult.REJECTED_RESETTING)
            return false;

        playGlobalSound(player.getServer(), ModSounds.MIH_START);
        clearPlayerRemainders(player.getUuid());
        ensureSnapshot(player);
        syncAll(player.getServer());
        return true;
    }

    public static boolean deactivate(ServerPlayerEntity player) {
        MadeInHeavenState.DeactivationResult result = STATE.deactivate(player.getUuid());
        if (result == MadeInHeavenState.DeactivationResult.NOT_ACTIVE)
            return false;
        clearPlayerRemainders(player.getUuid());
        syncAll(player.getServer());
        return true;
    }

    public static void tick(MinecraftServer server) {
        MadeInHeavenUniverseReset.tickPending(server);
        enrollEligiblePlayers(server);

        MadeInHeavenState.Phase phaseBefore = STATE.phase();
        int elapsedBefore = STATE.elapsedActiveTicks();
        Set<UUID> removed = STATE.removeInvalidUsers(playerId -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            return player == null || !isEligible(player);
        });
        for (UUID playerId : removed)
            clearPlayerRemainders(playerId);

        boolean theWorldActive = TheWorldAbility.isTimeStopped();
        MadeInHeavenState.TickResult result = STATE.tick(theWorldActive);
        boolean transition = STATE.phase() != phaseBefore;

        if (phaseBefore == MadeInHeavenState.Phase.BUILDUP
                && elapsedBefore < ENDING_SOUND_START_TICK
                && STATE.elapsedActiveTicks() >= ENDING_SOUND_START_TICK)
            playGlobalSound(server, ModSounds.MIH_ENDING);

        if (result == MadeInHeavenState.TickResult.RESET_REQUIRED
                && phaseBefore != MadeInHeavenState.Phase.RESETTING) {
            // Expose one ordered transition for the future reset cinematic before
            // applying and clearing the authoritative generation in this same tick.
            syncAll(server);
            MadeInHeavenUniverseReset.apply(server, STATE);
            clearAllPlayerRemainders();
            syncAll(server);
            return;
        }

        if (result == MadeInHeavenState.TickResult.COLLAPSE_COMPLETED)
            clearAllPlayerRemainders();

        if (!removed.isEmpty() || transition || result == MadeInHeavenState.TickResult.COLLAPSE_COMPLETED) {
            syncAll(server);
            return;
        }

        if (STATE.phase() != MadeInHeavenState.Phase.INACTIVE
                && server.getTicks() % CORRECTION_INTERVAL_TICKS == 0)
            syncAll(server);
    }

    public static void onJoin(ServerPlayerEntity player) {
        MadeInHeavenUniverseReset.tryRestorePending(player);
        if (STATE.phase() != MadeInHeavenState.Phase.INACTIVE && isEligible(player))
            ensureSnapshot(player);
        syncTo(player);
    }

    public static void onDisconnect(MinecraftServer server, ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        MadeInHeavenPhysicalController.clearPlayer(player);
        clearPlayerRemainders(playerId);
        MadeInHeavenState.DeactivationResult result = STATE.deactivate(playerId);
        if (result != MadeInHeavenState.DeactivationResult.NOT_ACTIVE)
            syncAll(server);
    }

    public static void onRespawn(
            ServerPlayerEntity oldPlayer,
            ServerPlayerEntity newPlayer,
            boolean alive
    ) {
        MadeInHeavenPhysicalController.clearPlayer(oldPlayer);
        clearPlayerRemainders(newPlayer.getUuid());
        if (!alive && STATE.isActiveUser(newPlayer.getUuid()))
            STATE.deactivate(newPlayer.getUuid());
        MadeInHeavenUniverseReset.tryRestorePending(newPlayer);
        if (STATE.phase() != MadeInHeavenState.Phase.INACTIVE
                && STATE.phase() != MadeInHeavenState.Phase.RESETTING
                && isEligible(newPlayer))
            ensureSnapshot(newPlayer);
        // Forced respawns during reset must not emit duplicate RESETTING packets.
        if (STATE.phase() != MadeInHeavenState.Phase.RESETTING)
            syncAll(newPlayer.getServer());
    }

    public static void onDimensionChanged(
            ServerPlayerEntity player,
            ServerWorld origin,
            ServerWorld destination
    ) {
        if (origin == destination)
            return;
        clearPlayerRemainders(player.getUuid());
        syncTo(player);
    }

    public static boolean isActiveUser(UUID playerId) {
        return STATE.isActiveUser(playerId);
    }

    public static MadeInHeavenState state() {
        return STATE;
    }

    public static void syncTo(ServerPlayerEntity player) {
        MadeInHeavenNetworking.sendState(
                player,
                STATE.synchronizedState(player.getServer().getTicks(), TheWorldAbility.isTimeStopped())
        );
    }

    public static void syncAll(MinecraftServer server) {
        MadeInHeavenSyncState synchronizedState = STATE.synchronizedState(
                server.getTicks(),
                TheWorldAbility.isTimeStopped()
        );
        MadeInHeavenNetworking.sendState(server, synchronizedState);
    }

    public static void reset() {
        STATE.clear();
        MadeInHeavenUniverseReset.clear();
    }

    private static void ensureSnapshot(ServerPlayerEntity player) {
        if (!STATE.snapshots().containsKey(player.getUuid()))
            STATE.enrollSnapshot(UniverseSnapshotAdapter.capture(player));
    }

    private static List<UniverseSnapshot> captureInitialSnapshots(MinecraftServer server) {
        List<UniverseSnapshot> snapshots = new ArrayList<>();
        for (ServerPlayerEntity candidate : server.getPlayerManager().getPlayerList()) {
            if (isEligible(candidate))
                snapshots.add(UniverseSnapshotAdapter.capture(candidate));
        }
        return snapshots;
    }

    private static void enrollEligiblePlayers(MinecraftServer server) {
        if (STATE.phase() == MadeInHeavenState.Phase.INACTIVE
                || STATE.phase() == MadeInHeavenState.Phase.RESETTING)
            return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isEligible(player))
                ensureSnapshot(player);
        }
    }

    private static boolean isEligible(ServerPlayerEntity player) {
        return player.isAlive() && !player.isSpectator();
    }

    private static void clearPlayerRemainders(UUID playerId) {
        TimeController.clearTemporalRemainders(playerId);
        TemporalActionController.clear(playerId);
    }

    private static void clearAllPlayerRemainders() {
        TimeController.clearTemporalRemainders();
        TemporalActionController.clear();
    }

    private static void playGlobalSound(MinecraftServer server, SoundEvent sound) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
            player.playSound(sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }
}
