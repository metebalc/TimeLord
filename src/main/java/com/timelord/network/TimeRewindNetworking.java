package com.timelord.network;

import com.timelord.TimeLord;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class TimeRewindNetworking {
    public static final Identifier EFFECT_PACKET = new Identifier(TimeLord.MOD_ID, "time_rewind_effect");

    private TimeRewindNetworking() {}

    public static void sendEffect(ServerPlayerEntity rewindingPlayer, Vec3d origin, Vec3d destination, int durationTicks) {
        ServerWorld world = rewindingPlayer.getServerWorld();

        for (ServerPlayerEntity viewer : world.getPlayers()) {
            if (viewer.squaredDistanceTo(destination) > 96.0D * 96.0D
                    && viewer.squaredDistanceTo(origin) > 96.0D * 96.0D)
                continue;

            PacketByteBuf buffer = PacketByteBufs.create();
            writeEffect(buffer, rewindingPlayer.getUuid(), origin, destination, durationTicks);
            ServerPlayNetworking.send(viewer, EFFECT_PACKET, buffer);
        }
    }

    private static void writeEffect(PacketByteBuf buffer, UUID playerId, Vec3d origin, Vec3d destination, int durationTicks) {
        buffer.writeUuid(playerId);
        buffer.writeDouble(origin.x);
        buffer.writeDouble(origin.y);
        buffer.writeDouble(origin.z);
        buffer.writeDouble(destination.x);
        buffer.writeDouble(destination.y);
        buffer.writeDouble(destination.z);
        buffer.writeVarInt(durationTicks);
    }
}
