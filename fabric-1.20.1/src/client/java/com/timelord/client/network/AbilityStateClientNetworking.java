package com.timelord.client.network;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.adapter.AbilityIdAdapter;
import com.timelord.client.state.ClientAbilityState;
import com.timelord.client.state.ClientFutureSightState;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.AbilityPacketCodec;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class AbilityStateClientNetworking {
    private AbilityStateClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.ABILITY_STATE,
                (client, handler, buffer, responseSender) -> {
                    AbilityMessages.AbilityStateUpdate message = AbilityPacketCodec.decodeAbilityState(buffer);
                    AbilityType ability = AbilityIdAdapter.toMinecraft(message.ability());

                    client.execute(() -> {
                        if (ability == null)
                            return;

                        ClientAbilityState.setActive(
                                ability, message.active(), message.remainingTicks(), message.totalTicks());
                        if (ability == AbilityType.FUTURE_SIGHT && !message.active())
                            ClientFutureSightState.clear();
                    });
                }
        );
    }
}
