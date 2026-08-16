package com.timelord.client.network;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.state.ClientAbilityLoadoutState;
import com.timelord.network.AbilityLoadoutNetworking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import net.minecraft.network.PacketByteBuf;

import java.util.EnumSet;

public final class AbilityLoadoutClientNetworking {
    private AbilityLoadoutClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                AbilityLoadoutNetworking.SYNC_PACKET,
                (client, handler, buffer, responseSender) -> {
                    int loadoutSize = buffer.readVarInt();
                    AbilityType[] loadout = new AbilityType[loadoutSize];

                    for (int slot = 0; slot < loadoutSize; slot++)
                        loadout[slot] = AbilityType.fromNetworkId(buffer.readByte());

                    int unlockedSize = buffer.readVarInt();
                    EnumSet<AbilityType> unlocked = EnumSet.noneOf(AbilityType.class);

                    for (int index = 0; index < unlockedSize; index++) {
                        AbilityType ability = AbilityType.fromNetworkId(buffer.readByte());
                        if (ability != null)
                            unlocked.add(ability);
                    }

                    client.execute(() -> ClientAbilityLoadoutState.set(loadout, unlocked));
                }
        );
    }

    public static void requestEquip(int slot, AbilityType ability) {
        if (!ClientPlayNetworking.canSend(AbilityLoadoutNetworking.EQUIP_PACKET))
            return;

        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(slot);
        buffer.writeByte(ability.networkId());
        ClientPlayNetworking.send(AbilityLoadoutNetworking.EQUIP_PACKET, buffer);
    }
}
