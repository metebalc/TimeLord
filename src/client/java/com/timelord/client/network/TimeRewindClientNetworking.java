package com.timelord.client.network;

import com.timelord.client.state.ClientTimeRewindState;
import com.timelord.network.TimeRewindNetworking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class TimeRewindClientNetworking {
    private TimeRewindClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                TimeRewindNetworking.EFFECT_PACKET,
                (client, handler, buffer, responseSender) -> {
                    UUID playerId = buffer.readUuid();
                    Vec3d origin = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
                    Vec3d destination = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
                    int durationTicks = buffer.readVarInt();

                    client.execute(() -> ClientTimeRewindState.start(playerId, origin, destination, durationTicks));
                }
        );
    }
}
