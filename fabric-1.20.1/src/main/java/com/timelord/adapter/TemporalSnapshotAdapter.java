package com.timelord.adapter;

import com.timelord.common.model.TemporalSnapshot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/** Captures Minecraft 1.20.1 player state into common temporal DTOs. */
public final class TemporalSnapshotAdapter {
    private TemporalSnapshotAdapter() {}

    public static TemporalSnapshot capture(ServerPlayerEntity player) {
        return new TemporalSnapshot(
                dimensionId(player.getWorld()),
                TemporalPositionAdapter.fromMinecraft(player.getPos()),
                player.getYaw(),
                player.getPitch(),
                TemporalPositionAdapter.fromMinecraft(player.getVelocity()),
                player.getHealth()
        );
    }

    public static String dimensionId(World world) {
        return world.getRegistryKey().getValue().toString();
    }
}
