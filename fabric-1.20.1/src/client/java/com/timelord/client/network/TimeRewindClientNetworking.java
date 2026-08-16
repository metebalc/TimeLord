package com.timelord.client.network;

import com.timelord.client.state.ClientTimeRewindState;
import com.timelord.common.network.message.TimeRewindMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.TimeRewindPacketCodec;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TimeRewindClientNetworking {
    private TimeRewindClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.TIME_REWIND_EFFECT,
                (client, handler, buffer, responseSender) -> {
                    TimeRewindMessages.Effect message = TimeRewindPacketCodec.decodeEffect(buffer);
                    client.execute(() -> ClientTimeRewindState.start(
                            message.playerId(), message.origin(), message.destination(), message.durationTicks()));
                }
        );
    }
}
