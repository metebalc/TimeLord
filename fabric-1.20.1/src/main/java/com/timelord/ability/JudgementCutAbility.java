package com.timelord.ability;

import com.timelord.network.JudgementCutNetworking;
import com.timelord.network.AbilityStateNetworking;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.ModSounds;

import com.timelord.time.TimeController;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class JudgementCutAbility implements ChargeableAbility {
    private static final double MIN_RADIUS = 2.5D;
    private static final double MAX_RADIUS = 10.0D;
    private static final long MAX_CHARGE_TIME_MS = 3000L;

    private static final float DAMAGE_PER_CUT = 4.0F;
    private static final int CUT_COUNT = 5;
    private static final int VISUAL_CUT_COUNT = 128;

    private static final int DETONATION_DELAY_TICKS = 60;
    private static final float JUDGEMENT_TIME_SCALE = 0.015F;

    private final Map<UUID, ChargeState> charging = new HashMap<>();
    private final Map<UUID, PendingJudgementCut> pendingCuts = new HashMap<>();

    public JudgementCutAbility() {}

    @Override
    public boolean startCharging(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (charging.containsKey(playerId) || pendingCuts.containsKey(playerId))
            return false;

        Vec3d lockedPosition = player.getPos();
        Vec3d sphereCenter = lockedPosition.add(0.0D, player.getHeight() * 0.5D, 0.0D);

        charging.put(playerId,
                new ChargeState(
                        System.currentTimeMillis(),
                        lockedPosition,
                        sphereCenter
                )
        );

        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.velocityModified = true;
        ServerWorld world = player.getServerWorld();

        world.playSound(
                null,
                player.getBlockPos(),
                ModSounds.JUDGEMENT_CHARGE,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        JudgementCutNetworking.sendStart(world, sphereCenter);
        AbilityStateNetworking.send(player, AbilityType.DIMENSION_CUT, true, 0, 0);
        return true;
    }

    @Override
    public void tickCharging(ServerPlayerEntity player) {
        ChargeState state = charging.get(player.getUuid());

        if (state == null)
            return;

        Vec3d locked = state.lockedPosition();

        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.teleport(locked.x, locked.y, locked.z);
        player.velocityModified = true;
    }

    @Override
    public boolean release(ServerPlayerEntity player) {
        ChargeState state = charging.remove(player.getUuid());

        if (state == null)
            return false;

        long heldTime = System.currentTimeMillis() - state.startTime();

        double progress = Math.min(1.0D, heldTime / (double) MAX_CHARGE_TIME_MS);
        progress = progress * progress * (3.0D - 2.0D * progress);
        double radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * progress;

        ServerWorld world = player.getServerWorld();

        Vec3d center = state.sphereCenter();

        long slashSeed = world.random.nextLong();

        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                ModSounds.JUDGEMENT_RELEASE,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        JudgementCutNetworking.sendRelease(world, radius, slashSeed, VISUAL_CUT_COUNT);
        JudgementCutNetworking.sendMonochrome(world, true);

        TimeController.addTemporaryField(player, center, radius, JUDGEMENT_TIME_SCALE, DETONATION_DELAY_TICKS);

        pendingCuts.put(player.getUuid(),
                new PendingJudgementCut(
                        world,
                        center,
                        radius,
                        DETONATION_DELAY_TICKS
                )
        );

        AbilityStateNetworking.send(
                player,
                AbilityType.DIMENSION_CUT,
                true,
                DETONATION_DELAY_TICKS,
                DETONATION_DELAY_TICKS
        );

        return true;
    }

    @Override
    public void cancelCharging(ServerPlayerEntity player) {
        ChargeState removed = charging.remove(player.getUuid());

        if (removed != null) {
            JudgementCutNetworking.sendClear(player.getServerWorld());
            AbilityStateNetworking.send(player, AbilityType.DIMENSION_CUT, false, 0, 0);
        }
    }

    @Override
    public void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PendingJudgementCut>> iterator = pendingCuts.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingJudgementCut> entry = iterator.next();

            UUID playerId = entry.getKey();

            PendingJudgementCut pending = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);

            if (player == null || !player.isAlive()) {
                TimeController.removeTemporaryField(playerId);

                JudgementCutNetworking.sendMonochrome(pending.world(), false);
                JudgementCutNetworking.sendClear(pending.world());

                if (player != null)
                    AbilityStateNetworking.send(player, AbilityType.DIMENSION_CUT, false, 0, 0);

                iterator.remove();
                continue;
            }

            int ticksLeft = pending.ticksRemaining() - 1;

            if (ticksLeft > 0) {
                entry.setValue(
                    new PendingJudgementCut(pending.world(), pending.center(), pending.radius(), ticksLeft)
                );
                continue;
            }

            TimeController.removeTemporaryField(player.getUuid());
            applyJudgementDamage(player, pending.center(), pending.radius());
            spawnDetonationEffect(pending.world(), pending.center(), pending.radius());

            JudgementCutNetworking.sendClear(pending.world());
            JudgementCutNetworking.sendMonochrome(pending.world(), false);
            AbilityStateNetworking.send(player, AbilityType.DIMENSION_CUT, false, 0, 0);

            iterator.remove();
        }
    }

    private static void applyJudgementDamage(ServerPlayerEntity player, Vec3d center, double radius) {
        ServerWorld world = player.getServerWorld();
        Box area = new Box(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );

        List<Entity> targets = world.getOtherEntities(player, area, entity ->
                        entity instanceof LivingEntity && entity.isAlive() && entity.squaredDistanceTo(center) <= radius * radius);

        float totalDamage = DAMAGE_PER_CUT * CUT_COUNT;

        for (Entity entity : targets) {
            LivingEntity target = (LivingEntity) entity;

            target.damage(world.getDamageSources().playerAttack(player), totalDamage);
        }
    }

    private static void spawnDetonationEffect(ServerWorld world, Vec3d center, double radius) {
        world.spawnParticles(
                ParticleTypes.REVERSE_PORTAL,
                center.x,
                center.y,
                center.z,
                80,
                radius * 0.45D,
                radius * 0.45D,
                radius * 0.45D,
                0.05D
        );

        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                ModSounds.JUDGEMENT_DETONATE,
                SoundCategory.PLAYERS,
                1.5F,
                1.0F
        );
    }

    public boolean isCharging(ServerPlayerEntity player) {
        return charging.containsKey(player.getUuid());
    }

    private record ChargeState(long startTime, Vec3d lockedPosition, Vec3d sphereCenter) { }

    private record PendingJudgementCut(ServerWorld world, Vec3d center, double radius, int ticksRemaining) {}

}
