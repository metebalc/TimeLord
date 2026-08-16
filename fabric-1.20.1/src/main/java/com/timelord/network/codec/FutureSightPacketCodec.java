package com.timelord.network.codec;

import com.timelord.common.model.ThreatInfo;
import com.timelord.common.model.ThreatType;
import com.timelord.common.network.message.FutureSightMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class FutureSightPacketCodec {
    private FutureSightPacketCodec() {}

    public static PacketByteBuf encode(FutureSightMessages.Threats message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(message.threats().size());
        for (ThreatInfo threat : message.threats()) {
            buffer.writeVarInt(threat.entityId());
            buffer.writeByte(threat.type().networkId());
        }
        return buffer;
    }

    public static FutureSightMessages.Threats decodeThreats(PacketByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ThreatInfo> threats = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int entityId = buffer.readVarInt();
            ThreatType type = ThreatType.fromNetworkId(buffer.readByte());
            if (type != null)
                threats.add(new ThreatInfo(entityId, type));
        }
        return new FutureSightMessages.Threats(threats);
    }
}
