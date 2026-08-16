package com.timelord.client.render;

import com.timelord.ModParticles;
import com.timelord.client.TimeLordClient;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class TimeShiftRenderer {
    private static final Random RANDOM = new Random();
    private static final double BASE_RADIUS = 0.65D;
    private static final double MAX_RADIUS = 1.20D;
    private static final double MIN_HEIGHT = 0.20D;
    private static final double MAX_HEIGHT = 1.75D;
    private static final int BASE_SEGMENTS = 4;
    private static final int MAX_SEGMENTS = 8;
    private static final int BURST_ARC_COUNT = 8;
    private static final int BURST_SEGMENTS = 9;
    private static final double BURST_RADIUS = 1.45D;
    private TimeShiftRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TimeShiftRenderer::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null)
            return;

        ClientPlayerEntity player = client.player;

        if (!player.isAlive() || player.isSpectator())
            return;

        int multiplier = TimeLordClient.getTimeShiftMultiplier();

        if (multiplier <= 0)
            return;

        if (TimeLordClient.isTimeShiftKeyDown()) {
            float holdProgress = TimeLordClient.getTimeShiftHoldProgress();

            spawnHoldElectricity(player, multiplier, holdProgress);
            spawnLightningTexture(player, multiplier, holdProgress);
        }

        if (TimeLordClient.isTimeShiftBursting()) {
            spawnBurstElectricity(player, multiplier);
        }
    }

    private static void spawnHoldElectricity(ClientPlayerEntity player, int multiplier, float progress) {
        float smooth = progress * progress * (3.0F - 2.0F * progress);

        if (smooth < 0.02F)
            return;

        int baseArcCount = getBaseArcCount(multiplier);
        int extraArcs = Math.round(smooth * getChargeArcBonus(multiplier));
        int arcCount = baseArcCount + extraArcs;
        int interval = getSpawnInterval(multiplier, smooth);

        if (player.age % interval != 0)
            return;

        double radius = lerp(BASE_RADIUS, MAX_RADIUS, smooth);
        int segments = BASE_SEGMENTS + Math.round(smooth * (MAX_SEGMENTS - BASE_SEGMENTS));

        for (int i = 0; i < arcCount; i++) {
            spawnElectricArc(player, radius, segments, false);
        }
    }

    private static void spawnBurstElectricity(ClientPlayerEntity player, int multiplier) {
        int interval = multiplier >= 10 ? 1 : 2;

        if (player.age % interval != 0)
            return;

        int arcCount = multiplier >= 10 ? BURST_ARC_COUNT + 3 : BURST_ARC_COUNT;
        for (int i = 0; i < arcCount; i++) {
            spawnElectricArc(player, BURST_RADIUS, BURST_SEGMENTS, true);
        }
    }

    private static void spawnElectricArc(ClientPlayerEntity player, double radius, int segments, boolean burst) {
        Vec3d start = randomPointAroundPlayer(player, radius * 0.55D);
        Vec3d end = randomPointAroundPlayer(player, radius);
        Vec3d direction = end.subtract(start);

        for (int segment = 0; segment <= segments; segment++) {
            double progress = segment / (double) segments;
            Vec3d point = start.add(direction.multiply(progress));
            double endpointFade = Math.sin(progress * Math.PI);
            double jitter = burst ? 0.18D : 0.11D;

            point = point.add(randomSigned(jitter * endpointFade), randomSigned(jitter * endpointFade), randomSigned(jitter * endpointFade));

            spawnElectricParticle(player, point, burst);
        }
    }

    private static void spawnElectricParticle(ClientPlayerEntity player, Vec3d position, boolean burst) {
        int count = burst && RANDOM.nextFloat() < 0.35F ? 2 : 1;

        for (int i = 0; i < count; i++) {
            player.getWorld()
                    .addParticle(
                            ParticleTypes.ELECTRIC_SPARK,

                            position.x,
                            position.y,
                            position.z,

                            randomSigned(burst ? 0.035D : 0.015D),
                            randomSigned(burst ? 0.035D : 0.015D),
                            randomSigned(burst ? 0.035D : 0.015D)
                    );
        }
    }

    private static void spawnLightningTexture(ClientPlayerEntity player, int multiplier, float progress) {
        float smooth = progress * progress * (3.0F - 2.0F * progress);

        if (smooth < 0.05F)
            return;

        int interval;

        if (multiplier >= 10) {
            interval = smooth >= 0.70F ? 1 : 2;
        } else if (multiplier >= 5) {
            interval = smooth >= 0.75F ? 2 : 3;
        } else if (multiplier >= 3) {
            interval = 3;
        } else {
            interval = 4;
        }

        if (player.age % interval != 0)
            return;

        int count = multiplier >= 10 ? 2 : 1;

        for (int i = 0; i < count; i++) {
            double radius = 0.15D + smooth * 0.25D;
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.85D + randomSigned(0.25D);

            player.getWorld().addParticle(ModParticles.TIME_SHIFT_LIGHTNING, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static Vec3d randomPointAroundPlayer(ClientPlayerEntity player, double radius) {
        double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
        double distance = radius * (0.35D + RANDOM.nextDouble() * 0.65D);
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        double y = player.getY() + MIN_HEIGHT + RANDOM.nextDouble() * (MAX_HEIGHT - MIN_HEIGHT);

        return new Vec3d(x, y, z);
    }

    private static int getBaseArcCount(int multiplier) {
        if (multiplier >= 10)
            return 3;

        if (multiplier >= 5)
            return 2;

        if (multiplier >= 3)
            return 1;

        return 1;
    }

    private static int getChargeArcBonus(int multiplier) {
        if (multiplier >= 10)
            return 5;

        if (multiplier >= 5)
            return 4;

        if (multiplier >= 3)
            return 3;

        return 2;
    }

    private static int getSpawnInterval(int multiplier, float charge) {
        if (charge >= 0.75F)
            return 1;

        if (multiplier >= 10)
            return 1;

        if (multiplier >= 5)
            return charge >= 0.35F ? 1 : 2;

        if (multiplier >= 3)
            return charge >= 0.50F ? 2 : 3;

        return 3;
    }

    private static double randomSigned(double amount) {
        return (RANDOM.nextDouble() * 2.0D - 1.0D) * amount;
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

}