package com.timelord.network;

import com.timelord.TimeLord;
import com.timelord.ability.AbilityLoadoutManager;
import com.timelord.ability.AbilityManager.AbilityType;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;

public final class AbilityLoadoutNetworking {
    public static final Identifier EQUIP_PACKET = new Identifier(TimeLord.MOD_ID, "equip_ability");
    public static final Identifier SYNC_PACKET = new Identifier(TimeLord.MOD_ID, "ability_loadout");

    private AbilityLoadoutNetworking() {}

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(
                EQUIP_PACKET,
                (server, player, handler, buffer, responseSender) -> {
                    int slot = buffer.readByte();
                    AbilityType ability = AbilityType.fromNetworkId(buffer.readByte());
                    server.execute(() -> AbilityLoadoutManager.equip(player, slot, ability));
                }
        );
    }

    public static void send(ServerPlayerEntity player, AbilityType[] loadout, Set<AbilityType> unlocked) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(loadout.length);

        for (AbilityType ability : loadout)
            buffer.writeByte(ability.networkId());

        buffer.writeVarInt(unlocked.size());
        for (AbilityType ability : unlocked)
            buffer.writeByte(ability.networkId());

        ServerPlayNetworking.send(player, SYNC_PACKET, buffer);
    }
}
