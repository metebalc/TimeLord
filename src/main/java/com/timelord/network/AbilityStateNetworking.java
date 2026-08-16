package com.timelord.network;

import com.timelord.TimeLord;
import com.timelord.ability.AbilityManager.AbilityType;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class AbilityStateNetworking {
    public static final Identifier STATE_PACKET = new Identifier(TimeLord.MOD_ID, "ability_state");

    private AbilityStateNetworking() {}

    public static void send(ServerPlayerEntity player, AbilityType ability, boolean active, int remainingTicks, int totalTicks) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(ability.networkId());
        buffer.writeBoolean(active);
        buffer.writeVarInt(Math.max(0, remainingTicks));
        buffer.writeVarInt(Math.max(0, totalTicks));
        ServerPlayNetworking.send(player, STATE_PACKET, buffer);
    }
}
