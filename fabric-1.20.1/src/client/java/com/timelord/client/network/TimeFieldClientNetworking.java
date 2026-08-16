package com.timelord.client.network;

import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.client.time.ClientTimeField;
import com.timelord.common.network.message.TimeFieldMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.TimeFieldPacketCodec;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TimeFieldClientNetworking {

    private TimeFieldClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.START_TIME_FIELD,
                (client, handler, buf, responseSender) -> {
                    TimeFieldMessages.Started message = TimeFieldPacketCodec.decodeStarted(buf);
                    client.execute(() -> ClientTimeField.set(
                            message.ownerId(),
                            TemporalPositionAdapter.toMinecraft(message.center()),
                            message.radius(),
                            message.durationTicks()
                    ));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.REMOVE_TIME_FIELD,
                (client, handler, buf, responseSender) -> {
                    TimeFieldMessages.Removed message = TimeFieldPacketCodec.decodeRemoved(buf);
                    client.execute(() -> ClientTimeField.remove(message.ownerId()));
                }
        );
    }
}
