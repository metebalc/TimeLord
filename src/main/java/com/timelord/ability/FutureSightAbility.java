package com.timelord.ability;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.future.ThreatDetector;
import com.timelord.network.AbilityStateNetworking;
import com.timelord.network.FutureSightNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public final class FutureSightAbility implements ToggleableAbility {
    private static final int SCAN_INTERVAL_TICKS = 4;
    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();
    private int scanTicker;

    @Override
    public boolean tryActivate(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (ACTIVE_PLAYERS.remove(playerId)) {
            AbilityStateNetworking.send(player, AbilityType.FUTURE_SIGHT, false, 0, 0);
            FutureSightNetworking.sendThreats(player, java.util.List.of());
            player.sendMessage(Text.literal("Future Sight: OFF"), true);
            return true;
        }

        if (TheWorldAbility.isTimeStopped() && !TheWorldAbility.canMove(player)) {
            player.sendMessage(Text.literal("Cannot use Future Sight while frozen in stopped time"), true);
            return false;
        }

        ACTIVE_PLAYERS.add(playerId);
        AbilityStateNetworking.send(player, AbilityType.FUTURE_SIGHT, true, 0, 0);
        player.sendMessage(Text.literal("Future Sight: ON"), true);
        return true;
    }

    @Override
    public boolean isActive(ServerPlayerEntity player) {
        return ACTIVE_PLAYERS.contains(player.getUuid());
    }

    public static void clear(UUID playerId) {
        ACTIVE_PLAYERS.remove(playerId);
    }

    @Override
    public void tick(MinecraftServer server) {
        Iterator<UUID> iterator = ACTIVE_PLAYERS.iterator();

        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);

            if (player == null || !player.isAlive() || player.isSpectator()) {
                iterator.remove();
                if (player != null) {
                    AbilityStateNetworking.send(player, AbilityType.FUTURE_SIGHT, false, 0, 0);
                    FutureSightNetworking.sendThreats(player, java.util.List.of());
                }
            }
        }

        scanTicker++;
        if (scanTicker % SCAN_INTERVAL_TICKS != 0)
            return;

        for (UUID playerId : ACTIVE_PLAYERS) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null)
                FutureSightNetworking.sendThreats(player, ThreatDetector.findThreats(player));
        }
    }
}
