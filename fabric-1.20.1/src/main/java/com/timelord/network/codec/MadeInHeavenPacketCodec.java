package com.timelord.network.codec;

import com.timelord.mih.MadeInHeavenState;
import com.timelord.mih.MadeInHeavenSyncState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class MadeInHeavenPacketCodec {
    private static final int MAX_ACTIVE_USERS = 1024;

    private MadeInHeavenPacketCodec() {}

    public static PacketByteBuf encode(MadeInHeavenSyncState state) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeLong(state.generationId());
        buffer.writeByte(state.phase().ordinal());
        buffer.writeVarInt(state.elapsedActiveTicks());
        buffer.writeVarInt(state.collapseElapsedTicks());
        buffer.writeVarInt(state.serverTick());
        buffer.writeBoolean(state.theWorldActive());
        buffer.writeVarInt(state.activeUsers().size());
        for (UUID playerId : state.activeUsers())
            buffer.writeUuid(playerId);
        return buffer;
    }

    public static MadeInHeavenSyncState decode(PacketByteBuf buffer) {
        long generationId = buffer.readLong();
        int phaseOrdinal = buffer.readUnsignedByte();
        MadeInHeavenState.Phase[] phases = MadeInHeavenState.Phase.values();
        if (phaseOrdinal >= phases.length)
            throw new IllegalArgumentException("Unknown Made in Heaven phase " + phaseOrdinal);

        int elapsedActiveTicks = buffer.readVarInt();
        int collapseElapsedTicks = buffer.readVarInt();
        int serverTick = buffer.readVarInt();
        boolean theWorldActive = buffer.readBoolean();
        int activeUserCount = buffer.readVarInt();
        if (activeUserCount < 0 || activeUserCount > MAX_ACTIVE_USERS)
            throw new IllegalArgumentException("Invalid Made in Heaven active user count " + activeUserCount);

        Set<UUID> activeUsers = new LinkedHashSet<>(activeUserCount);
        for (int index = 0; index < activeUserCount; index++)
            activeUsers.add(buffer.readUuid());

        return new MadeInHeavenSyncState(
                generationId,
                phases[phaseOrdinal],
                elapsedActiveTicks,
                collapseElapsedTicks,
                serverTick,
                theWorldActive,
                activeUsers
        );
    }
}
