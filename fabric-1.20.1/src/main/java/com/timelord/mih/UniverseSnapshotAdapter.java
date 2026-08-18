package com.timelord.mih;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Minecraft 1.20.1 boundary for duplication-safe Universe Snapshots. */
public final class UniverseSnapshotAdapter {
    private UniverseSnapshotAdapter() {}

    public static UniverseSnapshot capture(ServerPlayerEntity player) {
        Vec3d position = player.getPos();
        Vec3d velocity = player.getVelocity();
        return new UniverseSnapshot(
                player.getUuid(),
                player.getServerWorld().getRegistryKey().getValue().toString(),
                position.x,
                position.y,
                position.z,
                player.getYaw(),
                player.getPitch(),
                velocity.x,
                velocity.y,
                velocity.z,
                player.getHealth()
        );
    }
}
