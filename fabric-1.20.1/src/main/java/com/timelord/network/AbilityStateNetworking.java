package com.timelord.network;

import com.timelord.adapter.AbilityIdAdapter;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.network.codec.AbilityPacketCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AbilityStateNetworking {
    private AbilityStateNetworking() {}

    public static void send(ServerPlayerEntity player, AbilityType ability, boolean active, int remainingTicks, int totalTicks) {
        AbilityMessages.AbilityStateUpdate message = new AbilityMessages.AbilityStateUpdate(
                AbilityIdAdapter.toCommon(ability), active, remainingTicks, totalTicks);
        ServerPlayNetworking.send(player, PacketIds.ABILITY_STATE, AbilityPacketCodec.encode(message));
    }
}
