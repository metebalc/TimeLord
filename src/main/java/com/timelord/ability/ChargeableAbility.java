package com.timelord.ability;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ChargeableAbility extends Ability {
    boolean startCharging(ServerPlayerEntity player);
    void tickCharging(ServerPlayerEntity player);
    boolean release(ServerPlayerEntity player);
    default void cancelCharging(ServerPlayerEntity player) {}

    @Override
    default void activate(ServerPlayerEntity player) {
        startCharging(player);
    }
}