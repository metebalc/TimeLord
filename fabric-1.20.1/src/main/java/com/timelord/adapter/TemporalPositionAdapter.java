package com.timelord.adapter;

import com.timelord.common.model.TemporalPosition;
import net.minecraft.util.math.Vec3d;

/** Converts version-neutral temporal coordinates at the Minecraft boundary. */
public final class TemporalPositionAdapter {
    private TemporalPositionAdapter() {}

    public static TemporalPosition fromMinecraft(Vec3d position) {
        return new TemporalPosition(position.x, position.y, position.z);
    }

    public static Vec3d toMinecraft(TemporalPosition position) {
        return new Vec3d(position.x(), position.y(), position.z());
    }
}
