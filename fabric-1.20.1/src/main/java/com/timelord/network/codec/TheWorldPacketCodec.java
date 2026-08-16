package com.timelord.network.codec;

import com.timelord.common.network.message.TheWorldMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TheWorldPacketCodec {
    private TheWorldPacketCodec() {}

    public static PacketByteBuf encode(TheWorldMessages.State message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(message.activeDurations().size());
        for (Map.Entry<UUID, Integer> entry : message.activeDurations().entrySet()) {
            buffer.writeUuid(entry.getKey());
            buffer.writeVarInt(entry.getValue());
        }
        buffer.writeVarInt(message.maxDurationTicks());
        return buffer;
    }

    public static TheWorldMessages.State decodeState(PacketByteBuf buffer) {
        int count = buffer.readVarInt();
        Map<UUID, Integer> activeDurations = new LinkedHashMap<>();
        for (int index = 0; index < count; index++)
            activeDurations.put(buffer.readUuid(), buffer.readVarInt());
        return new TheWorldMessages.State(activeDurations, buffer.readVarInt());
    }

    public static PacketByteBuf encode(TheWorldMessages.Activation message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeUuid(message.activatorId());
        buffer.writeBoolean(message.globalTransition());
        return buffer;
    }

    public static TheWorldMessages.Activation decodeActivation(PacketByteBuf buffer) {
        return new TheWorldMessages.Activation(buffer.readUuid(), buffer.readBoolean());
    }

    public static PacketByteBuf encode(TheWorldMessages.StoredHit message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeUuid(message.hitId());
        buffer.writeUuid(message.targetId());
        buffer.writeUuid(message.attackerId());
        PacketCodecSupport.writePosition(buffer, message.impactPosition());
        PacketCodecSupport.writePosition(buffer, message.attackDirection());
        return buffer;
    }

    public static TheWorldMessages.StoredHit decodeStoredHit(PacketByteBuf buffer) {
        return new TheWorldMessages.StoredHit(
                buffer.readUuid(),
                buffer.readUuid(),
                buffer.readUuid(),
                PacketCodecSupport.readPosition(buffer),
                PacketCodecSupport.readPosition(buffer)
        );
    }

    public static PacketByteBuf encode(TheWorldMessages.ResolveHit message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeUuid(message.hitId());
        buffer.writeVarInt(message.sequenceIndex());
        buffer.writeVarInt(message.totalHits());
        return buffer;
    }

    public static TheWorldMessages.ResolveHit decodeResolveHit(PacketByteBuf buffer) {
        return new TheWorldMessages.ResolveHit(buffer.readUuid(), buffer.readVarInt(), buffer.readVarInt());
    }
}
