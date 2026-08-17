package com.timelord.client;

import com.timelord.ModParticles;
import com.timelord.client.particle.TimeShiftLightningParticle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public final class TimeLordClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleFactoryRegistry.getInstance().register(
				ModParticles.TIME_SHIFT_LIGHTNING,
				TimeShiftLightningParticle.Factory::new
		);
	}
}
