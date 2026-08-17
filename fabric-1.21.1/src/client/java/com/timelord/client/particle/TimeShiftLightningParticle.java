package com.timelord.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public final class TimeShiftLightningParticle extends SpriteBillboardParticle {
	private TimeShiftLightningParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z);
		setSprite(spriteProvider);

		velocityX = 0.0D;
		velocityY = 0.0D;
		velocityZ = 0.0D;
		maxAge = 4;
		scale = 1.2F;
		alpha = 0.9F;
		collidesWithWorld = false;
	}

	@Override
	public void tick() {
		super.tick();
		alpha = 1.0F - age / (float) maxAge;
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static final class Factory implements ParticleFactory<SimpleParticleType> {
		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				SimpleParticleType parameters,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ
		) {
			return new TimeShiftLightningParticle(world, x, y, z, spriteProvider);
		}
	}
}
