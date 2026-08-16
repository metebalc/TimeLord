package com.timelord.rewind;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public record PlayerStateSnapshot(
        RegistryKey<World> world,
        Vec3d position,
        float yaw,
        float pitch,
        Vec3d velocity,
        float health
) {}
