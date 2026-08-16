package com.timelord.network;

import com.timelord.common.model.PendingHit;
import com.timelord.common.network.message.TheWorldMessages;
import com.timelord.network.codec.TheWorldPacketCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

public final class TheWorldNetworking {
    private TheWorldNetworking() {}

    public static void sendActivation(MinecraftServer server, UUID activatorId, boolean globalTransition) {
        TheWorldMessages.Activation message = new TheWorldMessages.Activation(activatorId, globalTransition);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(player, PacketIds.THE_WORLD_ACTIVATE, TheWorldPacketCodec.encode(message));
    }

    public static void sendStoredHit(MinecraftServer server, PendingHit hit) {
        TheWorldMessages.StoredHit message = new TheWorldMessages.StoredHit(
                hit.hitId(), hit.targetId(), hit.attackerId(), hit.impactPosition(), hit.attackDirection());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(player, PacketIds.THE_WORLD_HIT, TheWorldPacketCodec.encode(message));
    }

    public static void sendResolveHit(
            MinecraftServer server,
            PendingHit hit,
            int sequenceIndex,
            int totalHits
    ) {
        TheWorldMessages.ResolveHit message =
                new TheWorldMessages.ResolveHit(hit.hitId(), sequenceIndex, totalHits);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(player, PacketIds.THE_WORLD_RESOLVE, TheWorldPacketCodec.encode(message));
    }

    public static void sendState(
            ServerPlayerEntity player,
            Map<UUID, Integer> activeDurations,
            int maxDurationTicks
    ) {
        TheWorldMessages.State message = new TheWorldMessages.State(activeDurations, maxDurationTicks);
        ServerPlayNetworking.send(player, PacketIds.THE_WORLD_STATE, TheWorldPacketCodec.encode(message));
    }
}
