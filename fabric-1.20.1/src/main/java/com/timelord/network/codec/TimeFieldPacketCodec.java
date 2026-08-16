package com.timelord.network.codec;

import com.timelord.common.network.message.TimeFieldMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class TimeFieldPacketCodec {
    private TimeFieldPacketCodec() {}

    public static PacketByteBuf encode(TimeFieldMessages.Started message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeUuid(message.ownerId());
        PacketCodecSupport.writePosition(buffer, message.center());
        buffer.writeDouble(message.radius());
        buffer.writeInt(message.durationTicks());
        return buffer;
    }

    public static TimeFieldMessages.Started decodeStarted(PacketByteBuf buffer) {
        return new TimeFieldMessages.Started(
                buffer.readUuid(),
                PacketCodecSupport.readPosition(buffer),
                buffer.readDouble(),
                buffer.readInt()
        );
    }

    public static PacketByteBuf encode(TimeFieldMessages.Removed message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeUuid(message.ownerId());
        return buffer;
    }

    public static TimeFieldMessages.Removed decodeRemoved(PacketByteBuf buffer) {
        return new TimeFieldMessages.Removed(buffer.readUuid());
    }
}
