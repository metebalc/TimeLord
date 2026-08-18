package com.timelord.client.network;

import com.timelord.client.time.MadeInHeavenClientState;
import com.timelord.mih.MadeInHeavenSyncState;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.MadeInHeavenPacketCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MadeInHeavenClientNetworking {
    private MadeInHeavenClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.MADE_IN_HEAVEN_STATE,
                (client, handler, buffer, responseSender) -> {
                    MadeInHeavenSyncState state = MadeInHeavenPacketCodec.decode(buffer);
                    client.execute(() -> MadeInHeavenClientState.apply(state));
                }
        );
    }
}
