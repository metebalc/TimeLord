package com.timelord.client.network;

import com.timelord.client.state.ClientFutureSightState;
import com.timelord.common.network.message.FutureSightMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.FutureSightPacketCodec;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FutureSightClientNetworking {
    private FutureSightClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.FUTURE_SIGHT_THREATS,
                (client, handler, buffer, responseSender) -> {
                    FutureSightMessages.Threats message = FutureSightPacketCodec.decodeThreats(buffer);
                    client.execute(() -> ClientFutureSightState.setThreats(message.threats()));
                }
        );
    }
}
