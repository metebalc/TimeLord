package com.timelord.client.network;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.adapter.AbilityIdAdapter;
import com.timelord.client.state.ClientAbilityState;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.AbilityPacketCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class AbilityClientNetworking {
    private AbilityClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.COOLDOWN,
                (client, handler, buffer, responseSender) -> {
                    AbilityMessages.CooldownUpdate message = AbilityPacketCodec.decodeCooldown(buffer);
                    AbilityType ability = AbilityIdAdapter.toMinecraft(message.ability());
                    client.execute(() -> {
                        if (ability != null)
                            ClientAbilityState.setCooldown(ability, message.remainingTicks());
                    });
                }
        );
    }

    public static void sendActivate(AbilityType ability) {
        if (!ClientPlayNetworking.canSend(PacketIds.ACTIVATE_ABILITY))
            return;

        AbilityMessages.ActivateAbility message = new AbilityMessages.ActivateAbility(ability.networkId());
        ClientPlayNetworking.send(PacketIds.ACTIVATE_ABILITY, AbilityPacketCodec.encode(message));
    }

    public static void sendStartCharge(AbilityType ability) {
        if (!ClientPlayNetworking.canSend(PacketIds.START_CHARGE))
            return;

        AbilityMessages.StartCharge message = new AbilityMessages.StartCharge(ability.networkId());
        ClientPlayNetworking.send(PacketIds.START_CHARGE, AbilityPacketCodec.encode(message));
    }

    public static void sendReleaseCharge() {
        if (ClientPlayNetworking.canSend(PacketIds.RELEASE_CHARGE))
            ClientPlayNetworking.send(PacketIds.RELEASE_CHARGE, PacketByteBufs.empty());
    }

    public static void sendSwitchSlowMode() {
        if (ClientPlayNetworking.canSend(PacketIds.SWITCH_SLOW_MODE))
            ClientPlayNetworking.send(PacketIds.SWITCH_SLOW_MODE, PacketByteBufs.empty());
    }
}
