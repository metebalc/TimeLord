package com.timelord.network;

import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.common.network.message.TimeFieldMessages;
import com.timelord.network.codec.TimeFieldPacketCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class TimeFieldNetworking {
    private TimeFieldNetworking() {
    }

    public static void sendStartField(
            MinecraftServer server,
            ServerWorld world,
            UUID owner,
            Vec3d center,
            double radius,
            int durationTicks
    ) {
        TimeFieldMessages.Started message = new TimeFieldMessages.Started(
                owner,
                TemporalPositionAdapter.fromMinecraft(center),
                radius,
                durationTicks
        );
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            ServerPlayNetworking.send(
                    player,
                    PacketIds.START_TIME_FIELD,
                    TimeFieldPacketCodec.encode(message)
            );
        }
    }

    public static void sendRemoveField(
            MinecraftServer server,
            UUID owner
    ) {
        TimeFieldMessages.Removed message = new TimeFieldMessages.Removed(owner);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(
                    player,
                    PacketIds.REMOVE_TIME_FIELD,
                    TimeFieldPacketCodec.encode(message)
            );
        }
    }
}
