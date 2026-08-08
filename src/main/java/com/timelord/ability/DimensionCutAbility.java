package com.timelord.ability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class DimensionCutAbility implements Ability {
    private static final double RANGE = 18.0D;
    private static final double HALF_WIDTH = 2.25D;
    private static final float DAMAGE = 12.0F;

    @Override
    public void activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d origin = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0F).normalize();
        Vec3d end = origin.add(direction.multiply(RANGE));

        Box searchBox = player.getBoundingBox().stretch(direction.multiply(RANGE)).expand(HALF_WIDTH);
        List<Entity> targets = world.getOtherEntities(player, searchBox,
                entity -> entity instanceof LivingEntity && entity.isAlive());

        for (Entity entity : targets) {
            Vec3d targetPoint = entity.getBoundingBox().getCenter();
            Vec3d offset = targetPoint.subtract(origin);
            double alongCut = offset.dotProduct(direction);
            if (alongCut < 0.0D || alongCut > RANGE) {
                continue;
            }

            Vec3d closestPoint = origin.add(direction.multiply(alongCut));
            if (targetPoint.squaredDistanceTo(closestPoint) <= HALF_WIDTH * HALF_WIDTH) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(world.getDamageSources().playerAttack(player), DAMAGE);
                target.takeKnockback(0.65D, -direction.x, -direction.z);
            }
        }

        for (double distance = 1.0D; distance <= RANGE; distance += 0.75D) {
            Vec3d point = origin.add(direction.multiply(distance));
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z,
                    1, 0.15D, 0.15D, 0.15D, 0.0D);
            world.spawnParticles(ParticleTypes.PORTAL, point.x, point.y, point.z,
                    2, 0.35D, 0.35D, 0.35D, 0.02D);
        }

        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, end.x, end.y, end.z,
                24, 0.6D, 0.6D, 0.6D, 0.05D);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 1.5F, 0.55F);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),
                SoundCategory.PLAYERS, 0.7F, 1.7F);
    }
}
