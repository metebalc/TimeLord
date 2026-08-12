package com.timelord.network;

import com.timelord.TimeLord;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class JudgementCutNetworking {
    private JudgementCutNetworking() {}

    public static void sendStart(ServerWorld world, Vec3d center) {
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            var buf = PacketByteBufs.create();

            buf.writeDouble(center.x);
            buf.writeDouble(center.y);
            buf.writeDouble(center.z);

            ServerPlayNetworking.send(player, TimeLord.JUDGEMENT_VISUAL_START_PACKET, buf);
        }
    }

    public static void sendRelease(ServerWorld world, double radius, long seed, int slashCount) {
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            var buf = PacketByteBufs.create();

            buf.writeDouble(radius);
            buf.writeLong(seed);
            buf.writeInt(slashCount);

            ServerPlayNetworking.send(player, TimeLord.JUDGEMENT_VISUAL_RELEASE_PACKET, buf);
        }
    }

    public static void sendClear(ServerWorld world) {
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            ServerPlayNetworking.send(player, TimeLord.JUDGEMENT_VISUAL_CLEAR_PACKET, PacketByteBufs.empty());
        }
    }

    public static void sendMonochrome(ServerWorld world, boolean active) {
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            var buf = PacketByteBufs.create();
            buf.writeBoolean(active);
            ServerPlayNetworking.send(player, TimeLord.JUDGEMENT_MONOCHROME_PACKET, buf);
        }
    }
}