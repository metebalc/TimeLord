package com.timelord.client.judgement;

import net.minecraft.util.math.Vec3d;

public record SlashInstance(
        Vec3d position,
        float yaw,
        float pitch,
        float roll,
        float length,
        float width,
        float intensity
) {
}