package com.timelord.network.codec;

import com.timelord.common.network.message.TimeShiftMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class TimeShiftPacketCodec {
    private TimeShiftPacketCodec() {}

    public static PacketByteBuf encode(TimeShiftMessages.State message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(message.active());
        buffer.writeInt(message.multiplier());
        return buffer;
    }

    public static TimeShiftMessages.State decodeState(PacketByteBuf buffer) {
        return new TimeShiftMessages.State(buffer.readBoolean(), buffer.readInt());
    }
}
