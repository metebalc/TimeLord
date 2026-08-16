package com.timelord.client.network;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.adapter.AbilityIdAdapter;
import com.timelord.client.state.ClientAbilityLoadoutState;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.AbilityPacketCodec;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.EnumSet;

public final class AbilityLoadoutClientNetworking {
    private AbilityLoadoutClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.ABILITY_LOADOUT,
                (client, handler, buffer, responseSender) -> {
                    AbilityMessages.LoadoutSync message = AbilityPacketCodec.decodeLoadout(buffer);
                    AbilityType[] loadout = message.equipped().stream()
                            .map(AbilityIdAdapter::toMinecraft)
                            .toArray(AbilityType[]::new);
                    EnumSet<AbilityType> unlocked = EnumSet.noneOf(AbilityType.class);

                    for (var abilityId : message.unlocked()) {
                        AbilityType ability = AbilityIdAdapter.toMinecraft(abilityId);
                        if (ability != null)
                            unlocked.add(ability);
                    }

                    client.execute(() -> ClientAbilityLoadoutState.set(loadout, unlocked));
                }
        );
    }

    public static void requestEquip(int slot, AbilityType ability) {
        if (!ClientPlayNetworking.canSend(PacketIds.EQUIP_ABILITY))
            return;

        AbilityMessages.EquipAbility message =
                new AbilityMessages.EquipAbility(slot, AbilityIdAdapter.toCommon(ability));
        ClientPlayNetworking.send(PacketIds.EQUIP_ABILITY, AbilityPacketCodec.encode(message));
    }
}
