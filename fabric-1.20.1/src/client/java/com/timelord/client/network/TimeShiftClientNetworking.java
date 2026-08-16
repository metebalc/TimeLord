package com.timelord.client.network;

import com.timelord.client.TimeLordClient;
import com.timelord.common.network.message.TimeShiftMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.TimeShiftPacketCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class TimeShiftClientNetworking {
    private TimeShiftClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.TIME_SHIFT_STATE,
                (client, handler, buffer, responseSender) -> {
                    TimeShiftMessages.State message = TimeShiftPacketCodec.decodeState(buffer);
                    client.execute(() -> TimeLordClient.applyTimeShiftState(
                            client, message.active(), message.multiplier()));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.TIME_SHIFT_BURST,
                (client, handler, buffer, responseSender) ->
                        client.execute(TimeLordClient::applyTimeShiftBurst)
        );
    }

    public static boolean canSendStartCharge() {
        return ClientPlayNetworking.canSend(PacketIds.TIME_SHIFT_CHARGE_START);
    }

    public static void sendStartCharge() {
        ClientPlayNetworking.send(PacketIds.TIME_SHIFT_CHARGE_START, PacketByteBufs.empty());
    }

    public static void sendRelease() {
        if (ClientPlayNetworking.canSend(PacketIds.TIME_SHIFT_RELEASE))
            ClientPlayNetworking.send(PacketIds.TIME_SHIFT_RELEASE, PacketByteBufs.empty());
    }
}
