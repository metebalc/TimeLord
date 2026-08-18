package com.timelord.mih;

import com.timelord.ability.AbilityManager;
import com.timelord.ability.TimeShiftAbility;
import com.timelord.rewind.SafeRewindPositionFinder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Initial duplication-safe Universe Reset implementation for Minecraft 1.20.1. */
public final class MadeInHeavenUniverseReset {
    private static final PendingUniverseRestores PENDING_RESTORES =
            new PendingUniverseRestores();

    private MadeInHeavenUniverseReset() {}

    public static void apply(MinecraftServer server, MadeInHeavenState state) {
        Set<UUID> preservedUsers = new LinkedHashSet<>(state.activeUsers());
        Map<UUID, UniverseSnapshot> snapshots = new LinkedHashMap<>(state.snapshots());

        for (Map.Entry<UUID, UniverseSnapshot> entry : snapshots.entrySet()) {
            UUID playerId = entry.getKey();
            if (preservedUsers.contains(playerId)) {
                PENDING_RESTORES.preserve(playerId);
                continue;
            }

            UniverseSnapshot snapshot = entry.getValue();
            PENDING_RESTORES.stage(snapshot, state.generationId());
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null)
                continue;

            if (!player.isAlive()) {
                ServerPlayerEntity respawned = server.getPlayerManager().respawnPlayer(player, false);
                tryRestorePending(respawned);
            } else if (!player.isSpectator()) {
                tryRestorePending(player);
            }
        }

        state.completeReset();
    }

    public static boolean tryRestorePending(ServerPlayerEntity player) {
        return tryRestorePending(player, true);
    }

    public static void tickPending(MinecraftServer server) {
        for (UUID playerId : PENDING_RESTORES.playerIds()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null && player.isAlive() && !player.isSpectator())
                tryRestorePending(player, false);
        }
    }

    private static boolean tryRestorePending(ServerPlayerEntity player, boolean force) {
        if (!player.isAlive() || player.isSpectator())
            return false;
        UniverseSnapshot snapshot = PENDING_RESTORES.beginAttempt(
                player.getUuid(), player.getServer().getTicks(), force);
        if (snapshot == null)
            return false;

        Optional<ResetDestination> destination = findDestination(player, snapshot);
        if (destination.isEmpty())
            return false;

        ResetDestination target = destination.get();
        AbilityManager.cancelCharging(player);
        TimeShiftAbility.cancelTransientState(player);
        player.stopRiding();
        if (player.isSleeping())
            player.wakeUp(true, true);
        player.closeHandledScreen();
        player.setPortalCooldown(player.getDefaultPortalCooldown());
        player.teleport(
                target.world(),
                target.position().x,
                target.position().y,
                target.position().z,
                snapshot.yaw(),
                snapshot.pitch()
        );
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.setHealth(Math.max(1.0F, Math.min(snapshot.health(), player.getMaxHealth())));
        player.fallDistance = 0.0F;
        PENDING_RESTORES.complete(player.getUuid());
        return true;
    }

    public static void clear() {
        PENDING_RESTORES.clear();
    }

    private static Optional<ResetDestination> findDestination(
            ServerPlayerEntity player,
            UniverseSnapshot snapshot
    ) {
        MinecraftServer server = player.getServer();
        ServerWorld snapshotWorld = findWorld(server, snapshot.dimensionId());
        Vec3d preferred = new Vec3d(snapshot.x(), snapshot.y(), snapshot.z());

        Optional<ResetDestination> destination = findSafe(snapshotWorld, player, preferred);
        if (destination.isPresent())
            return destination;

        if (snapshotWorld != null) {
            destination = findSafe(snapshotWorld, player, Vec3d.ofBottomCenter(snapshotWorld.getSpawnPos()));
            if (destination.isPresent())
                return destination;
        }

        ServerWorld currentWorld = player.getServerWorld();
        destination = findSafe(currentWorld, player, player.getPos());
        if (destination.isPresent())
            return destination;

        destination = findSafe(currentWorld, player, Vec3d.ofBottomCenter(currentWorld.getSpawnPos()));
        if (destination.isPresent())
            return destination;

        ServerWorld overworld = server.getOverworld();
        return findSafe(overworld, player, Vec3d.ofBottomCenter(overworld.getSpawnPos()));
    }

    private static Optional<ResetDestination> findSafe(
            ServerWorld world,
            ServerPlayerEntity player,
            Vec3d preferred
    ) {
        if (world == null)
            return Optional.empty();
        return SafeRewindPositionFinder.find(world, player, preferred)
                .map(position -> new ResetDestination(world, position));
    }

    private static ServerWorld findWorld(MinecraftServer server, String dimensionId) {
        Identifier identifier = Identifier.tryParse(dimensionId);
        if (identifier == null)
            return null;
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, identifier);
        return server.getWorld(worldKey);
    }

    private record ResetDestination(ServerWorld world, Vec3d position) {}
}
