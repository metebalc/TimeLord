package com.timelord.network;

import com.timelord.common.model.ThreatInfo;
import com.timelord.common.network.message.FutureSightMessages;
import com.timelord.network.codec.FutureSightPacketCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class FutureSightNetworking {
    private FutureSightNetworking() {}

    public static void sendThreats(ServerPlayerEntity player, List<ThreatInfo> threats) {
        FutureSightMessages.Threats message = new FutureSightMessages.Threats(threats);
        ServerPlayNetworking.send(player, PacketIds.FUTURE_SIGHT_THREATS, FutureSightPacketCodec.encode(message));
    }
}
