package com.timelord.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class TimeFieldNetworking {

    public static final Identifier START_FIELD =
            new Identifier("timelord", "start_time_field");

    public static final Identifier REMOVE_FIELD =
            new Identifier("timelord", "remove_time_field");

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
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            var buf = PacketByteBufs.create();

            buf.writeUuid(owner);
            buf.writeDouble(center.x);
            buf.writeDouble(center.y);
            buf.writeDouble(center.z);
            buf.writeDouble(radius);
            buf.writeInt(durationTicks);

            ServerPlayNetworking.send(
                    player,
                    START_FIELD,
                    buf
            );
        }
    }

    public static void sendRemoveField(
            MinecraftServer server,
            UUID owner
    ) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            var buf = PacketByteBufs.create();
            buf.writeUuid(owner);
            ServerPlayNetworking.send(
                    player,
                    REMOVE_FIELD,
                    buf
            );
        }
    }
}