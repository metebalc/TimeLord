package com.timelord.network.codec;

import com.timelord.common.model.TemporalPosition;
import net.minecraft.network.PacketByteBuf;

final class PacketCodecSupport {
    private PacketCodecSupport() {}

    static void writePosition(PacketByteBuf buffer, TemporalPosition position) {
        buffer.writeDouble(position.x());
        buffer.writeDouble(position.y());
        buffer.writeDouble(position.z());
    }

    static TemporalPosition readPosition(PacketByteBuf buffer) {
        return new TemporalPosition(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
