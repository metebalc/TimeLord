package com.timelord.mih;

import com.timelord.ability.TheWorldAbility;
import com.timelord.ability.TimeShiftAbility;
import com.timelord.time.TimeController;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Applies the authoritative locomotion component without skipping complete player ticks. */
public final class MadeInHeavenPhysicalController {
    private static final UUID MOVEMENT_SCALE_UUID =
            UUID.fromString("f317ca4c-d0ec-4892-8ce6-24628dbbb748");
    private static final double UPDATE_EPSILON = 0.0025D;
    private static final Map<UUID, Double> APPLIED_FACTORS = new HashMap<>();

    private MadeInHeavenPhysicalController() {}

    public static void tick(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            onlinePlayers.add(player.getUuid());
            if (player.isSpectator()) {
                removeModifier(player);
                continue;
            }

            applyMovementFactor(player, movementAttributeFactor(player));
        }

        APPLIED_FACTORS.keySet().retainAll(onlinePlayers);
    }

    public static double temporalScale(ServerPlayerEntity player) {
        return temporalState(player).scale();
    }

    public static TemporalState temporalState(ServerPlayerEntity player) {
        return temporalResolution(player).state();
    }

    public static TemporalResolver.PlayerResolution temporalResolution(
            ServerPlayerEntity player
    ) {
        MadeInHeavenState state = MadeInHeavenServerController.state();
        boolean madeInHeavenActive = state.phase() != MadeInHeavenState.Phase.INACTIVE;
        return TemporalResolver.resolvePlayerPolicy(new TemporalResolver.PlayerContext(
                TheWorldAbility.isTimeStopped(),
                TheWorldAbility.canMove(player),
                madeInHeavenActive,
                madeInHeavenActive && state.isActiveUser(player.getUuid()),
                state.elapsedActiveTicks(),
                TimeShiftAbility.getMovementMultiplier(player),
                TimeController.slowTimeFactor(player.getServerWorld(), player)
        ));
    }

    public static void clear(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
            removeModifier(player);
        APPLIED_FACTORS.clear();
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        removeModifier(player);
    }

    private static double movementAttributeFactor(ServerPlayerEntity player) {
        return temporalResolution(player).movementAttributeFactor();
    }

    private static void applyMovementFactor(ServerPlayerEntity player, double factor) {
        Double previous = APPLIED_FACTORS.get(player.getUuid());
        if (previous != null && Math.abs(previous - factor) < UPDATE_EPSILON)
            return;

        EntityAttributeInstance movementSpeed =
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed == null)
            return;

        movementSpeed.removeModifier(MOVEMENT_SCALE_UUID);
        if (factor < 0.9999D) {
            movementSpeed.addTemporaryModifier(new EntityAttributeModifier(
                    MOVEMENT_SCALE_UUID,
                    "Resolved temporal movement scale",
                    factor - 1.0D,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            ));
            APPLIED_FACTORS.put(player.getUuid(), factor);
        } else {
            APPLIED_FACTORS.remove(player.getUuid());
        }
    }

    private static void removeModifier(ServerPlayerEntity player) {
        EntityAttributeInstance movementSpeed =
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null)
            movementSpeed.removeModifier(MOVEMENT_SCALE_UUID);
        APPLIED_FACTORS.remove(player.getUuid());
    }

}
