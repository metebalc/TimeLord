package com.timelord.network.codec;

import com.timelord.common.network.message.JudgementCutMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class JudgementCutPacketCodec {
    private JudgementCutPacketCodec() {}

    public static PacketByteBuf encode(JudgementCutMessages.Start message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        PacketCodecSupport.writePosition(buffer, message.center());
        return buffer;
    }

    public static JudgementCutMessages.Start decodeStart(PacketByteBuf buffer) {
        return new JudgementCutMessages.Start(PacketCodecSupport.readPosition(buffer));
    }

    public static PacketByteBuf encode(JudgementCutMessages.Release message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeDouble(message.radius());
        buffer.writeLong(message.seed());
        buffer.writeInt(message.slashCount());
        return buffer;
    }

    public static JudgementCutMessages.Release decodeRelease(PacketByteBuf buffer) {
        return new JudgementCutMessages.Release(buffer.readDouble(), buffer.readLong(), buffer.readInt());
    }

    public static PacketByteBuf encode(JudgementCutMessages.Monochrome message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(message.active());
        return buffer;
    }

    public static JudgementCutMessages.Monochrome decodeMonochrome(PacketByteBuf buffer) {
        return new JudgementCutMessages.Monochrome(buffer.readBoolean());
    }
}
