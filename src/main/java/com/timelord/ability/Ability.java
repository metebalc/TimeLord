package com.timelord.ability;

import net.minecraft.server.network.ServerPlayerEntity;

public interface Ability {
    void activate(ServerPlayerEntity player);
    default void deactivate(ServerPlayerEntity player){};
}
