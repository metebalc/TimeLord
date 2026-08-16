package com.timelord.network;

import com.timelord.ability.AbilityLoadoutManager;
import com.timelord.ability.AbilityManager;
import com.timelord.ability.TimeShiftAbility;
import com.timelord.common.network.message.TimeShiftMessages;
import com.timelord.network.codec.TimeShiftPacketCodec;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TimeShiftNetworking {
    private TimeShiftNetworking() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
                PacketIds.TIME_SHIFT_CHARGE_START,
                (server, player, handler, buffer, responseSender) -> server.execute(() -> {
                    if (AbilityLoadoutManager.isEquipped(player, AbilityManager.AbilityType.TIME_SHIFT))
                        TimeShiftAbility.startLaunchCharge(player);
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PacketIds.TIME_SHIFT_RELEASE,
                (server, player, handler, buffer, responseSender) ->
                        server.execute(() -> TimeShiftAbility.release(player))
        );
    }

    public static void sendState(ServerPlayerEntity player, boolean active, int multiplier) {
        TimeShiftMessages.State message = new TimeShiftMessages.State(active, multiplier);
        ServerPlayNetworking.send(player, PacketIds.TIME_SHIFT_STATE, TimeShiftPacketCodec.encode(message));
    }

    public static void sendBurst(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, PacketIds.TIME_SHIFT_BURST, PacketByteBufs.empty());
    }
}
