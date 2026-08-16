package com.timelord.adapter;

import com.timelord.common.logic.PendingHitResolver.DamageContribution;
import com.timelord.common.logic.PendingHitResolver.ResolutionPlan;
import com.timelord.common.logic.PendingHitResolver.TargetResolution;
import com.timelord.common.model.PendingHit;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/** Applies a common pending-hit resolution plan through Minecraft 1.20.1 APIs. */
public final class TheWorldDamageAdapter {
    private TheWorldDamageAdapter() {}

    public static void apply(MinecraftServer server, ResolutionPlan plan) {
        for (TargetResolution targetResolution : plan.targets()) {
            Entity target = findEntity(server, targetResolution.targetId());

            if (target == null || !target.isAlive())
                continue;

            for (DamageContribution contribution : targetResolution.damageContributions()) {
                ServerPlayerEntity attacker = server.getPlayerManager().getPlayer(contribution.attackerId());
                if (attacker == null)
                    continue;

                target.timeUntilRegen = 0;
                target.damage(
                        target.getDamageSources().playerAttack(attacker),
                        contribution.damage()
                );

                if (!target.isAlive())
                    break;
            }

            PendingHit lastHit = targetResolution.lastHit();
            if (target.isAlive()) {
                applyFinalKnockback(target, TemporalPositionAdapter.toMinecraft(lastHit.attackDirection()));
                playFinalResolveEffect(target);
            }
        }
    }

    private static void applyFinalKnockback(Entity target, Vec3d attackDirection) {
        Vec3d horizontal = new Vec3d(attackDirection.x, 0.0D, attackDirection.z);
        if (horizontal.lengthSquared() < 0.0001D)
            return;

        horizontal = horizontal.normalize();
        Vec3d velocity = target.getVelocity();

        target.setVelocity(
                velocity.x + horizontal.x * 0.65D,
                Math.max(velocity.y, 0.22D),
                velocity.z + horizontal.z * 0.65D
        );
        target.velocityModified = true;
    }

    private static void playFinalResolveEffect(Entity target) {
        ServerWorld world = (ServerWorld) target.getWorld();
        world.spawnParticles(
                ParticleTypes.CRIT,
                target.getX(),
                target.getBodyY(0.55D),
                target.getZ(),
                18,
                0.35D,
                0.55D,
                0.35D,
                0.18D
        );
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null)
                return entity;
        }

        return null;
    }
}
