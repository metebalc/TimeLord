package com.timelord.network;

import com.timelord.ability.AbilityManager;
import com.timelord.ability.SlowTimeAbility;
import com.timelord.adapter.AbilityIdAdapter;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.network.codec.AbilityPacketCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AbilityNetworking {
    private AbilityNetworking() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
                PacketIds.ACTIVATE_ABILITY,
                (server, player, handler, buffer, responseSender) -> {
                    AbilityMessages.ActivateAbility message = AbilityPacketCodec.decodeActivate(buffer);
                    server.execute(() -> AbilityManager.activate(player, message.abilityNetworkId()));
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PacketIds.START_CHARGE,
                (server, player, handler, buffer, responseSender) -> {
                    AbilityMessages.StartCharge message = AbilityPacketCodec.decodeStartCharge(buffer);
                    server.execute(() -> AbilityManager.startCharging(player, message.abilityNetworkId()));
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PacketIds.RELEASE_CHARGE,
                (server, player, handler, buffer, responseSender) ->
                        server.execute(() -> AbilityManager.releaseCharging(player))
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PacketIds.SWITCH_SLOW_MODE,
                (server, player, handler, buffer, responseSender) ->
                        server.execute(() -> SlowTimeAbility.switchMode(player))
        );
    }

    public static void sendCooldown(
            ServerPlayerEntity player,
            AbilityManager.AbilityType ability,
            int remainingTicks
    ) {
        AbilityMessages.CooldownUpdate message = new AbilityMessages.CooldownUpdate(
                AbilityIdAdapter.toCommon(ability),
                remainingTicks
        );
        ServerPlayNetworking.send(player, PacketIds.COOLDOWN, AbilityPacketCodec.encode(message));
    }
}
