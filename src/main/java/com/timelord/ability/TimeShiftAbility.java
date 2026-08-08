package com.timelord.ability;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class TimeShiftAbility implements Ability {
    private static final int DURATION_TICKS = 7 * 20;

    @Override
    public void activate(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION_TICKS, 5, false, false, false));

        ServerWorld world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getBodyY(0.5D), player.getZ(),
                40, 0.5D, 0.9D, 0.5D, 0.08D);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.8F, 1.6F);
    }
}
