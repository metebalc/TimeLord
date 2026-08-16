package com.timelord.network;

import com.timelord.TimeLord;
import com.timelord.future.ThreatDetector.Threat;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public final class FutureSightNetworking {
    public static final Identifier THREATS_PACKET = new Identifier(TimeLord.MOD_ID, "future_sight_threats");

    private FutureSightNetworking() {}

    public static void sendThreats(ServerPlayerEntity player, List<Threat> threats) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(threats.size());

        for (Threat threat : threats) {
            buffer.writeVarInt(threat.entityId());
            buffer.writeByte(threat.type().networkId());
        }

        ServerPlayNetworking.send(player, THREATS_PACKET, buffer);
    }
}
