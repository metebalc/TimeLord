package com.timelord.rewind;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlayerStateHistory {
    private static final int HISTORY_SIZE = 80;
    private static final Map<UUID, History> HISTORIES = new HashMap<>();

    private PlayerStateHistory() {}

    public static void tick(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            onlinePlayers.add(playerId);

            if (!player.isAlive() || player.isSpectator()) {
                HISTORIES.remove(playerId);
                continue;
            }

            History history = HISTORIES.get(playerId);
            if (history == null || history.player != player || !history.world.equals(player.getWorld().getRegistryKey())) {
                history = new History(player);
                HISTORIES.put(playerId, history);
            }

            history.snapshots.addLast(capture(player));
            while (history.snapshots.size() > HISTORY_SIZE)
                history.snapshots.removeFirst();
        }

        HISTORIES.keySet().removeIf(playerId -> !onlinePlayers.contains(playerId));
    }

    public static Optional<PlayerStateSnapshot> get(ServerPlayerEntity player, int ticksAgo) {
        History history = HISTORIES.get(player.getUuid());

        if (history == null || history.player != player || history.snapshots.size() <= ticksAgo)
            return Optional.empty();

        int index = history.snapshots.size() - 1 - ticksAgo;
        return Optional.of(new ArrayList<>(history.snapshots).get(index));
    }

    private static PlayerStateSnapshot capture(ServerPlayerEntity player) {
        return new PlayerStateSnapshot(
                player.getWorld().getRegistryKey(),
                player.getPos(),
                player.getYaw(),
                player.getPitch(),
                player.getVelocity(),
                player.getHealth()
        );
    }

    private static final class History {
        private final ServerPlayerEntity player;
        private final net.minecraft.registry.RegistryKey<net.minecraft.world.World> world;
        private final Deque<PlayerStateSnapshot> snapshots = new ArrayDeque<>();

        private History(ServerPlayerEntity player) {
            this.player = player;
            this.world = player.getWorld().getRegistryKey();
        }
    }
}
