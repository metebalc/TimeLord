package com.timelord.network;

import com.timelord.mih.MadeInHeavenSyncState;
import com.timelord.network.codec.MadeInHeavenPacketCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MadeInHeavenNetworking {
    private MadeInHeavenNetworking() {}

    public static void sendState(ServerPlayerEntity player, MadeInHeavenSyncState state) {
        ServerPlayNetworking.send(
                player,
                PacketIds.MADE_IN_HEAVEN_STATE,
                MadeInHeavenPacketCodec.encode(state)
        );
    }

    public static void sendState(MinecraftServer server, MadeInHeavenSyncState state) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
            sendState(player, state);
    }
}
