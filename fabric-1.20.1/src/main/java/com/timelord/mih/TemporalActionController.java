package com.timelord.mih;

import com.timelord.ability.TheWorldAbility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative cadence gates for time-sensitive player actions. */
public final class TemporalActionController {
    private static final Map<UUID, Integer> LAST_ACCEPTED_ATTACK_TICK = new HashMap<>();
    private static final FractionalTickAccumulator ITEM_USE_TICKS = new FractionalTickAccumulator();
    private static final FractionalTickAccumulator JUMP_TICKS = new FractionalTickAccumulator();

    private TemporalActionController() {}

    public static boolean canAttack(ServerPlayerEntity player) {
        TemporalState state = MadeInHeavenPhysicalController.temporalState(player);
        if (state.isStopped())
            return false;

        double actionScale = TemporalActionCadence.actionScale(state);
        if (actionScale >= 0.999999D) {
            LAST_ACCEPTED_ATTACK_TICK.remove(player.getUuid());
            return true;
        }

        int requiredTicks = TemporalActionCadence.attackIntervalTicks(
                player.getAttackCooldownProgressPerTick(), state);
        int now = player.getServer().getTicks();
        Integer lastAccepted = LAST_ACCEPTED_ATTACK_TICK.get(player.getUuid());
        if (lastAccepted != null && now - lastAccepted < requiredTicks)
            return false;

        LAST_ACCEPTED_ATTACK_TICK.put(player.getUuid(), now);
        return true;
    }

    public static boolean shouldAdvanceItemUse(ServerPlayerEntity player) {
        return shouldAdvanceFractionalAction(player, ITEM_USE_TICKS);
    }

    public static boolean canJump(ServerPlayerEntity player) {
        return shouldAdvanceFractionalAction(player, JUMP_TICKS);
    }

    private static boolean shouldAdvanceFractionalAction(
            ServerPlayerEntity player,
            FractionalTickAccumulator accumulator
    ) {
        TemporalState state = MadeInHeavenPhysicalController.temporalState(player);
        if (state.isStopped())
            return false;

        // During The World, resistant MIH users already receive only fractional whole
        // entity ticks. Applying another fractional gate here would square the slowdown.
        if (TheWorldAbility.isTimeStopped())
            return true;

        return accumulator.shouldStep(
                player.getUuid(), TemporalActionCadence.actionScale(state));
    }

    public static void tick(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
            onlinePlayers.add(player.getUuid());

        LAST_ACCEPTED_ATTACK_TICK.keySet().retainAll(onlinePlayers);
        ITEM_USE_TICKS.retainAll(onlinePlayers);
        JUMP_TICKS.retainAll(onlinePlayers);
    }

    public static void clear() {
        LAST_ACCEPTED_ATTACK_TICK.clear();
        ITEM_USE_TICKS.clear();
        JUMP_TICKS.clear();
    }

    public static void clear(UUID playerId) {
        LAST_ACCEPTED_ATTACK_TICK.remove(playerId);
        ITEM_USE_TICKS.remove(playerId);
        JUMP_TICKS.remove(playerId);
    }
}
