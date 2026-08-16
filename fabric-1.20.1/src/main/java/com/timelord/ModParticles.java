package com.timelord;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {
    public static final DefaultParticleType TIME_SHIFT_LIGHTNING = FabricParticleTypes.simple();

    private ModParticles() {}

    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE,
                new Identifier(TimeLord.MOD_ID, "time_shift_lightning"), TIME_SHIFT_LIGHTNING);
    }

}