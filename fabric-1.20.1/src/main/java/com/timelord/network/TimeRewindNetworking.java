package com.timelord.network;

import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.common.network.message.TimeRewindMessages;
import com.timelord.network.codec.TimeRewindPacketCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class TimeRewindNetworking {
    private TimeRewindNetworking() {}

    public static void sendEffect(ServerPlayerEntity rewindingPlayer, Vec3d origin, Vec3d destination, int durationTicks) {
        ServerWorld world = rewindingPlayer.getServerWorld();
        TimeRewindMessages.Effect message = new TimeRewindMessages.Effect(
                rewindingPlayer.getUuid(),
                TemporalPositionAdapter.fromMinecraft(origin),
                TemporalPositionAdapter.fromMinecraft(destination),
                durationTicks
        );

        for (ServerPlayerEntity viewer : world.getPlayers()) {
            if (viewer.squaredDistanceTo(destination) > 96.0D * 96.0D
                    && viewer.squaredDistanceTo(origin) > 96.0D * 96.0D)
                continue;

            ServerPlayNetworking.send(viewer, PacketIds.TIME_REWIND_EFFECT, TimeRewindPacketCodec.encode(message));
        }
    }
}
