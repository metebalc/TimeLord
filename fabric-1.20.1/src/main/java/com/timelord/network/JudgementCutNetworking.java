package com.timelord.network;

import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.common.network.message.JudgementCutMessages;
import com.timelord.network.codec.JudgementCutPacketCodec;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class JudgementCutNetworking {
    private JudgementCutNetworking() {}

    public static void sendStart(ServerWorld world, Vec3d center) {
        JudgementCutMessages.Start message =
                new JudgementCutMessages.Start(TemporalPositionAdapter.fromMinecraft(center));
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            ServerPlayNetworking.send(
                    player, PacketIds.JUDGEMENT_VISUAL_START, JudgementCutPacketCodec.encode(message));
        }
    }

    public static void sendRelease(ServerWorld world, double radius, long seed, int slashCount) {
        JudgementCutMessages.Release message = new JudgementCutMessages.Release(radius, seed, slashCount);
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            ServerPlayNetworking.send(
                    player, PacketIds.JUDGEMENT_VISUAL_RELEASE, JudgementCutPacketCodec.encode(message));
        }
    }

    public static void sendClear(ServerWorld world) {
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            ServerPlayNetworking.send(player, PacketIds.JUDGEMENT_VISUAL_CLEAR, PacketByteBufs.empty());
        }
    }

    public static void sendMonochrome(ServerWorld world, boolean active) {
        JudgementCutMessages.Monochrome message = new JudgementCutMessages.Monochrome(active);
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != world)
                continue;

            ServerPlayNetworking.send(
                    player, PacketIds.JUDGEMENT_MONOCHROME, JudgementCutPacketCodec.encode(message));
        }
    }
}
