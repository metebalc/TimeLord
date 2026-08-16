package com.timelord.client.network;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.state.ClientAbilityState;
import com.timelord.client.state.ClientFutureSightState;
import com.timelord.network.AbilityStateNetworking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class AbilityStateClientNetworking {
    private AbilityStateClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                AbilityStateNetworking.STATE_PACKET,
                (client, handler, buffer, responseSender) -> {
                    AbilityType ability = AbilityType.fromNetworkId(buffer.readByte());
                    boolean active = buffer.readBoolean();
                    int remainingTicks = buffer.readVarInt();
                    int totalTicks = buffer.readVarInt();

                    client.execute(() -> {
                        if (ability == null)
                            return;

                        ClientAbilityState.setActive(ability, active, remainingTicks, totalTicks);
                        if (ability == AbilityType.FUTURE_SIGHT && !active)
                            ClientFutureSightState.clear();
                    });
                }
        );
    }
}
