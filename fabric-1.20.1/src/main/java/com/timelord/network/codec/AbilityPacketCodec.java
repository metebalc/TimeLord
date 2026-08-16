package com.timelord.network.codec;

import com.timelord.common.ability.AbilityId;
import com.timelord.common.network.message.AbilityMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class AbilityPacketCodec {
    private AbilityPacketCodec() {}

    public static PacketByteBuf encode(AbilityMessages.ActivateAbility message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(message.abilityNetworkId());
        return buffer;
    }

    public static AbilityMessages.ActivateAbility decodeActivate(PacketByteBuf buffer) {
        return new AbilityMessages.ActivateAbility(buffer.readByte());
    }

    public static PacketByteBuf encode(AbilityMessages.StartCharge message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(message.abilityNetworkId());
        return buffer;
    }

    public static AbilityMessages.StartCharge decodeStartCharge(PacketByteBuf buffer) {
        return new AbilityMessages.StartCharge(buffer.readByte());
    }

    public static PacketByteBuf encode(AbilityMessages.CooldownUpdate message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(message.ability().networkId());
        buffer.writeInt(message.remainingTicks());
        return buffer;
    }

    public static AbilityMessages.CooldownUpdate decodeCooldown(PacketByteBuf buffer) {
        return new AbilityMessages.CooldownUpdate(
                AbilityId.fromNetworkId(buffer.readByte()),
                buffer.readInt()
        );
    }

    public static PacketByteBuf encode(AbilityMessages.EquipAbility message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(message.slot());
        buffer.writeByte(message.ability().networkId());
        return buffer;
    }

    public static AbilityMessages.EquipAbility decodeEquip(PacketByteBuf buffer) {
        return new AbilityMessages.EquipAbility(
                buffer.readByte(),
                AbilityId.fromNetworkId(buffer.readByte())
        );
    }

    public static PacketByteBuf encode(AbilityMessages.LoadoutSync message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(message.equipped().size());
        for (AbilityId ability : message.equipped())
            buffer.writeByte(ability.networkId());

        buffer.writeVarInt(message.unlocked().size());
        for (AbilityId ability : message.unlocked())
            buffer.writeByte(ability.networkId());
        return buffer;
    }

    public static AbilityMessages.LoadoutSync decodeLoadout(PacketByteBuf buffer) {
        int equippedCount = buffer.readVarInt();
        List<AbilityId> equipped = new ArrayList<>(equippedCount);
        for (int index = 0; index < equippedCount; index++)
            equipped.add(AbilityId.fromNetworkId(buffer.readByte()));

        int unlockedCount = buffer.readVarInt();
        List<AbilityId> unlocked = new ArrayList<>(unlockedCount);
        for (int index = 0; index < unlockedCount; index++)
            unlocked.add(AbilityId.fromNetworkId(buffer.readByte()));
        return new AbilityMessages.LoadoutSync(equipped, unlocked);
    }

    public static PacketByteBuf encode(AbilityMessages.AbilityStateUpdate message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(message.ability().networkId());
        buffer.writeBoolean(message.active());
        buffer.writeVarInt(Math.max(0, message.remainingTicks()));
        buffer.writeVarInt(Math.max(0, message.totalTicks()));
        return buffer;
    }

    public static AbilityMessages.AbilityStateUpdate decodeAbilityState(PacketByteBuf buffer) {
        return new AbilityMessages.AbilityStateUpdate(
                AbilityId.fromNetworkId(buffer.readByte()),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }
}
