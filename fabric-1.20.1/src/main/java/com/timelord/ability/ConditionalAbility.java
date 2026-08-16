package com.timelord.ability;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ConditionalAbility extends Ability {
    boolean tryActivate(ServerPlayerEntity player);

    @Override
    default void activate(ServerPlayerEntity player) {
        tryActivate(player);
    }
}
