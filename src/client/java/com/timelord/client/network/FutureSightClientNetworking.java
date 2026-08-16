package com.timelord.client.network;

import com.timelord.client.state.ClientFutureSightState;
import com.timelord.future.ThreatType;
import com.timelord.network.FutureSightNetworking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FutureSightClientNetworking {
    private FutureSightClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                FutureSightNetworking.THREATS_PACKET,
                (client, handler, buffer, responseSender) -> {
                    int count = buffer.readVarInt();
                    Map<Integer, ThreatType> threats = new LinkedHashMap<>();

                    for (int index = 0; index < count; index++) {
                        int entityId = buffer.readVarInt();
                        ThreatType type = ThreatType.fromNetworkId(buffer.readByte());
                        if (type != null)
                            threats.put(entityId, type);
                    }

                    client.execute(() -> ClientFutureSightState.setThreats(threats));
                }
        );
    }
}
