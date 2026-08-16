package com.timelord.network;

import com.timelord.adapter.AbilityIdAdapter;
import com.timelord.ability.AbilityLoadoutManager;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.common.ability.AbilityId;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.network.codec.AbilityPacketCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class AbilityLoadoutNetworking {
    private AbilityLoadoutNetworking() {}

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(
                PacketIds.EQUIP_ABILITY,
                (server, player, handler, buffer, responseSender) -> {
                    AbilityMessages.EquipAbility message = AbilityPacketCodec.decodeEquip(buffer);
                    server.execute(() -> AbilityLoadoutManager.equip(
                            player,
                            message.slot(),
                            AbilityIdAdapter.toMinecraft(message.ability())
                    ));
                }
        );
    }

    public static void send(ServerPlayerEntity player, AbilityType[] loadout, Set<AbilityType> unlocked) {
        List<AbilityId> equippedIds = Arrays.stream(loadout).map(AbilityIdAdapter::toCommon).toList();
        List<AbilityId> unlockedIds = unlocked.stream().map(AbilityIdAdapter::toCommon).toList();
        AbilityMessages.LoadoutSync message = new AbilityMessages.LoadoutSync(equippedIds, unlockedIds);
        ServerPlayNetworking.send(player, PacketIds.ABILITY_LOADOUT, AbilityPacketCodec.encode(message));
    }
}
