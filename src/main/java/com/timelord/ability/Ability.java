package com.timelord.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public interface Ability {
    void activate(ServerPlayerEntity player);
    default void deactivate(MinecraftServer server, ServerPlayerEntity player){};
    default void tick(MinecraftServer server) {}
}
