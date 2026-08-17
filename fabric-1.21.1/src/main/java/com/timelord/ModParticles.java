package com.timelord;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
	public static final SimpleParticleType TIME_SHIFT_LIGHTNING = FabricParticleTypes.simple();

	private ModParticles() {
	}

	public static void register() {
		Registry.register(
				Registries.PARTICLE_TYPE,
				TimeLord.id("time_shift_lightning"),
				TIME_SHIFT_LIGHTNING
		);
	}
}
