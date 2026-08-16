package com.timelord.network.codec;

import com.timelord.common.network.message.TimeRewindMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class TimeRewindPacketCodec {
    private TimeRewindPacketCodec() {}

    public static PacketByteBuf encode(TimeRewindMessages.Effect message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeUuid(message.playerId());
        PacketCodecSupport.writePosition(buffer, message.origin());
        PacketCodecSupport.writePosition(buffer, message.destination());
        buffer.writeVarInt(message.durationTicks());
        return buffer;
    }

    public static TimeRewindMessages.Effect decodeEffect(PacketByteBuf buffer) {
        return new TimeRewindMessages.Effect(
                buffer.readUuid(),
                PacketCodecSupport.readPosition(buffer),
                PacketCodecSupport.readPosition(buffer),
                buffer.readVarInt()
        );
    }
}
