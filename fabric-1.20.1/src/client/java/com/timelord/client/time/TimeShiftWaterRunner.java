package com.timelord.client.time;

import com.timelord.client.TimeLordClient;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class TimeShiftWaterRunner {
    private static final int MIN_WATER_RUN_MULTIPLIER = 5;
    private static final double MIN_HORIZONTAL_SPEED_SQUARED = 0.035D;
    private static double lastLandHorizontalSpeed = 0.0D;
    private static double waterRunBaselineSpeed = 0.0D;
    private static boolean wasWaterRunning = false;
    private static final double MAX_WATER_SEARCH_DEPTH = 0.75D;
    private static final double SURFACE_CATCH_ABOVE = 0.20D;
    private static final double SURFACE_CATCH_BELOW = 0.15D;
    private static final double SURFACE_OFFSET = 0.001D;
    private static final double LAND_WATER_CHECK_DEPTH = 0.20D;
    private static final float X5_LIMB_SPEED = 1.0F;
    private static final float X10_LIMB_SPEED = 1.25F;

    private TimeShiftWaterRunner() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TimeShiftWaterRunner::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            resetState();
            return;
        }

        ClientPlayerEntity player = client.player;

        int multiplier = TimeLordClient.getTimeShiftMultiplier();

        if (multiplier < MIN_WATER_RUN_MULTIPLIER) {
            resetState();
            return;
        }

        if (!player.isAlive() || player.isSpectator()) {
            resetState();
            return;
        }

        captureLandSpeed(player);

        boolean runningOnWater = tryWaterRun(player, multiplier);

        if (wasWaterRunning && !runningOnWater)
            waterRunBaselineSpeed = 0.0D;

        wasWaterRunning = runningOnWater;
    }

    private static void captureLandSpeed(ClientPlayerEntity player) {
        if (wasWaterRunning)
            return;

        if (!hasMovementInput(player))
            return;

        if (!player.isOnGround())
            return;

        if (isWaterNearFeet(player, LAND_WATER_CHECK_DEPTH)) {
            return;
        }

        Vec3d velocity =
                player.getVelocity();

        double horizontalSpeed =
                Math.sqrt(
                        velocity.x * velocity.x
                                + velocity.z * velocity.z
                );

        if (horizontalSpeed
                < 0.01D) {

            return;
        }

        lastLandHorizontalSpeed =
                horizontalSpeed;
    }

    private static boolean tryWaterRun(
            ClientPlayerEntity player,
            int multiplier
    ) {

        Vec3d velocity =
                player.getVelocity();

        Vec3d horizontalVelocity =
                new Vec3d(
                        velocity.x,
                        0.0D,
                        velocity.z
                );

        double horizontalSpeedSquared =
                horizontalVelocity
                        .lengthSquared();

        if (!hasMovementInput(
                player
        )) {

            return false;
        }

        if (horizontalSpeedSquared
                < MIN_HORIZONTAL_SPEED_SQUARED) {

            return false;
        }

        if (player.isSubmergedInWater()) {

            return false;
        }

        WaterSurface surface =
                findWaterSurface(
                        player
                );

        if (surface == null) {

            return false;
        }

        double targetY =
                surface.surfaceY()
                        + SURFACE_OFFSET;

        double playerY =
                player.getY();

        boolean nearSurface =
                playerY
                        <= targetY
                        + SURFACE_CATCH_ABOVE
                        &&
                        playerY
                                >= targetY
                                - SURFACE_CATCH_BELOW;

        if (!nearSurface) {

            return false;
        }

        if (velocity.y > 0.0D) {

            return false;
        }

        if (!wasWaterRunning) {

            if (lastLandHorizontalSpeed
                    > 0.01D) {

                waterRunBaselineSpeed =
                        lastLandHorizontalSpeed;

            } else {

                waterRunBaselineSpeed =
                        horizontalVelocity.length();
            }
        }

        player.setSwimming(
                false
        );

        player.setOnGround(
                true
        );

        player.setSprinting(
                true
        );

        Vec3d maintainedHorizontal =
                calculateWaterVelocity(
                        horizontalVelocity
                );

        player.setPosition(
                player.getX(),
                targetY,
                player.getZ()
        );

        player.setVelocity(
                maintainedHorizontal.x,
                0.0D,
                maintainedHorizontal.z
        );

        player.fallDistance =
                0.0F;

        updateRunningAnimation(
                player,
                multiplier,
                maintainedHorizontal
        );

        spawnWaterTrail(
                player,
                surface.surfaceY(),
                maintainedHorizontal,
                multiplier
        );

        return true;
    }

    private static Vec3d calculateWaterVelocity(
            Vec3d horizontalVelocity
    ) {

        double currentSpeed =
                horizontalVelocity.length();

        if (currentSpeed
                < 0.001D) {

            return horizontalVelocity;
        }

        double targetSpeed =
                waterRunBaselineSpeed
                        > 0.01D
                        ? waterRunBaselineSpeed
                        : currentSpeed;

        if (TimeLordClient
                .isTimeShiftBursting()) {

            targetSpeed =
                    Math.max(
                            targetSpeed,
                            currentSpeed
                    );
        }

        return horizontalVelocity
                .normalize()
                .multiply(
                        targetSpeed
                );
    }

    private static boolean hasMovementInput(
            ClientPlayerEntity player
    ) {

        return Math.abs(
                player.input
                        .movementForward
        ) > 0.001F

                ||

                Math.abs(
                        player.input
                                .movementSideways
                ) > 0.001F;
    }

    private static void updateRunningAnimation(
            ClientPlayerEntity player,
            int multiplier,
            Vec3d horizontalVelocity
    ) {

        double speed =
                horizontalVelocity.length();

        if (speed
                < 0.01D) {

            return;
        }

        float limbSpeed =
                multiplier >= 10
                        ? X10_LIMB_SPEED
                        : X5_LIMB_SPEED;

        player.limbAnimator.updateLimbs(
                limbSpeed,
                1.0F
        );
    }

    private static WaterSurface findWaterSurface(
            ClientPlayerEntity player
    ) {

        final double[] offsets = {
                0.01D,
                0.10D,
                0.20D,
                0.35D,
                0.50D,
                0.75D
        };

        for (double offset : offsets) {

            if (offset
                    > MAX_WATER_SEARCH_DEPTH) {

                break;
            }

            BlockPos pos =
                    BlockPos.ofFloored(
                            player.getX(),
                            player.getY()
                                    - offset,
                            player.getZ()
                    );

            FluidState fluid =
                    player.getWorld()
                            .getFluidState(
                                    pos
                            );

            if (!fluid.isIn(
                    FluidTags.WATER
            )) {

                continue;
            }

            double surfaceY =
                    pos.getY()
                            + 1.0D;

            return new WaterSurface(
                    pos,
                    surfaceY
            );
        }

        return null;
    }

    private static boolean isWaterNearFeet(
            ClientPlayerEntity player,
            double maxDepth
    ) {

        final double[] offsets = {
                0.01D,
                0.05D,
                0.10D,
                0.15D,
                0.20D
        };

        for (double offset : offsets) {

            if (offset > maxDepth) {
                break;
            }

            BlockPos pos =
                    BlockPos.ofFloored(
                            player.getX(),
                            player.getY()
                                    - offset,
                            player.getZ()
                    );

            if (player.getWorld()
                    .getFluidState(
                            pos
                    )
                    .isIn(
                            FluidTags.WATER
                    )) {

                return true;
            }
        }

        return false;
    }

    private static void spawnWaterTrail(
            ClientPlayerEntity player,
            double surfaceY,
            Vec3d horizontalVelocity,
            int multiplier
    ) {

        if (multiplier < 10
                && player.age % 2 != 0) {

            return;
        }

        Vec3d direction =
                horizontalVelocity
                        .lengthSquared()
                        > 0.001D
                        ? horizontalVelocity.normalize()
                        : Vec3d.ZERO;

        boolean leftStep =
                (player.age / 2)
                        % 2 == 0;

        Vec3d side =
                new Vec3d(
                        -direction.z,
                        0.0D,
                        direction.x
                );

        double sideOffset =
                leftStep
                        ? 0.18D
                        : -0.18D;

        Vec3d particlePosition =
                new Vec3d(
                        player.getX(),
                        surfaceY
                                + 0.02D,
                        player.getZ()
                );

        particlePosition =
                particlePosition
                        .add(
                                direction.multiply(
                                        -0.20D
                                )
                        )
                        .add(
                                side.multiply(
                                        sideOffset
                                )
                        );

        int particleCount =
                multiplier >= 10
                        ? 5
                        : 3;

        for (
                int i = 0;
                i < particleCount;
                i++
        ) {

            double spreadX =
                    (
                            player.getRandom()
                                    .nextDouble()
                                    - 0.5D
                    ) * 0.18D;

            double spreadZ =
                    (
                            player.getRandom()
                                    .nextDouble()
                                    - 0.5D
                    ) * 0.18D;

            double backwardsX =
                    -direction.x
                            * 0.09D;

            double backwardsZ =
                    -direction.z
                            * 0.09D;

            player.getWorld()
                    .addParticle(
                            ParticleTypes.SPLASH,

                            particlePosition.x
                                    + spreadX,

                            particlePosition.y,

                            particlePosition.z
                                    + spreadZ,

                            backwardsX,

                            0.10D,

                            backwardsZ
                    );
        }
    }

    private static void resetState() {

        lastLandHorizontalSpeed =
                0.0D;

        waterRunBaselineSpeed =
                0.0D;

        wasWaterRunning =
                false;
    }

    private record WaterSurface(
            BlockPos blockPos,
            double surfaceY
    ) {
    }
}
