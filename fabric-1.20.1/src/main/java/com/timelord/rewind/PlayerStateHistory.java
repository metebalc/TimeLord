package com.timelord.rewind;

import com.timelord.adapter.TemporalSnapshotAdapter;
import com.timelord.common.model.TemporalSnapshot;
import com.timelord.common.state.TimeRewindHistory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlayerStateHistory {
    private static final int HISTORY_SIZE = 80;
    private static final TimeRewindHistory HISTORY = new TimeRewindHistory(HISTORY_SIZE);

    private PlayerStateHistory() {}

    public static void tick(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            onlinePlayers.add(playerId);

            if (!player.isAlive() || player.isSpectator()) {
                HISTORY.reset(playerId);
                continue;
            }

            HISTORY.record(playerId, TemporalSnapshotAdapter.capture(player));
        }

        HISTORY.retainPlayers(onlinePlayers);
    }

    public static Optional<TemporalSnapshot> get(ServerPlayerEntity player, int ticksAgo) {
        return HISTORY.get(
                player.getUuid(),
                TemporalSnapshotAdapter.dimensionId(player.getWorld()),
                ticksAgo
        );
    }

    public static void reset(UUID playerId) {
        HISTORY.reset(playerId);
    }

    public static void clear() {
        HISTORY.clear();
    }
}
