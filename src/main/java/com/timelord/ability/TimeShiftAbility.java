package com.timelord.ability;

import com.timelord.ModSounds;
import com.timelord.TimeLord;
import com.timelord.mixin.EntityStepHeightAccessor;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TimeShiftAbility implements Ability {
    private static final UUID TIME_SHIFT_SPEED_UUID = UUID.fromString("a8275680-5e1c-4e61-88db-93af224dd531");
    private static final UUID TIME_SHIFT_BURST_UUID = UUID.fromString("7ae05d18-61ad-468c-a597-a282db2a89a5");

    private static final Map<UUID, Mode> PLAYER_MODES = new HashMap<>();
    private static final Map<UUID, Long> LAUNCH_CHARGES = new HashMap<>();
    private static final Map<UUID, Vec3d> CHARGE_POSITIONS = new HashMap<>();
    private static final Map<UUID, BurstState> ACTIVE_BURSTS = new HashMap<>();

    private static final Map<UUID, Double> LAST_LAND_SPEED = new HashMap<>();

    private static final Map<UUID, Double> WATER_BASELINE_SPEED = new HashMap<>();

    private static final Set<UUID> WATER_RUNNING_PLAYERS = new HashSet<>();

    private static final float NORMAL_STEP_HEIGHT = 0.6F;

    private static final float TIME_SHIFT_STEP_HEIGHT = 1.25F;


    private static final double BURST_MULTIPLIER = 2.0D;
    private static final int BURST_DURATION_TICKS = 14;
    private static final long SERVER_FULL_CHARGE_TIME_MS = 200L;
    private static final double MIN_LAUNCH_SPEED = 1.75D;
    private static final double MAX_LAUNCH_SPEED = 3.0D;
    private static final double LAUNCH_VERTICAL_BOOST = 0.12D;

    private static final int MIN_WATER_RUN_MULTIPLIER = 5;

    private static final double WATER_RUN_MIN_SPEED_SQUARED = 0.035D;
    private static final double MAX_WATER_SEARCH_DEPTH = 0.75D;
    private static final double LAND_WATER_CHECK_DEPTH = 0.20D;
    private static final double SURFACE_CATCH_ABOVE = 0.20D;
    private static final double SURFACE_CATCH_BELOW = 0.15D;
    private static final double WATER_SURFACE_OFFSET = 0.001D;

    private enum Mode {
        OFF(0),
        X2(2),
        X3(3),
        X5(5),
        X10(10);

        private final int multiplier;

        Mode(int multiplier) {
            this.multiplier = multiplier;
        }

        public int multiplier() {
            return multiplier;
        }
    }

    @Override
    public void activate(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        Mode currentMode = PLAYER_MODES.getOrDefault(playerId, Mode.OFF);
        Mode nextMode = switch (currentMode) {
            case OFF -> Mode.X2;
            case X2 -> Mode.X3;
            case X3 -> Mode.X5;
            case X5 -> Mode.X10;
            case X10 -> Mode.OFF;
        };

        PLAYER_MODES.put(playerId, nextMode);

        applyMode(player, nextMode);

        if (nextMode == Mode.OFF) {
            LAUNCH_CHARGES.remove(playerId);
            CHARGE_POSITIONS.remove(playerId);

            removeBurst(player);
            clearWaterState(playerId);
        }
    }

    public static void startLaunchCharge(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        Mode mode = PLAYER_MODES.getOrDefault(playerId, Mode.OFF);

        if (mode == Mode.OFF)
            return;


        if (!LAUNCH_CHARGES.containsKey(playerId)) {
            LAUNCH_CHARGES.put(playerId, System.currentTimeMillis());
            CHARGE_POSITIONS.put(playerId, player.getPos());
        }
    }

    public static void release(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        Long chargeStart = LAUNCH_CHARGES.remove(playerId);

        CHARGE_POSITIONS.remove(playerId);

        if (chargeStart == null)
            return;

        Mode mode = PLAYER_MODES.getOrDefault(playerId, Mode.OFF);

        if (mode == Mode.OFF)
            return;

        long elapsed = Math.max(0L, System.currentTimeMillis() - chargeStart);
        float finalCharge = 0.8F + 0.2F * Math.min(1.0F, elapsed / (float) SERVER_FULL_CHARGE_TIME_MS);

        activateBurst(player, finalCharge);
    }

    private static void tickLaunchCharges(MinecraftServer server) {
        for (UUID playerId : LAUNCH_CHARGES.keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);

            if (player == null)
                continue;

            if (!player.isAlive() || player.isSpectator())
                continue;

            lockPlayerDuringCharge(player);
        }
    }

    private static void lockPlayerDuringCharge(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        Vec3d anchor = CHARGE_POSITIONS.get(playerId);

        if (anchor == null)
            return;

        Vec3d velocity = player.getVelocity();

        player.setVelocity(0.0D, velocity.y, 0.0D);
        player.velocityModified = true;
        player.setSprinting(false);

        player.teleport(anchor.x, player.getY(), anchor.z);
    }

    @Override
    public void tick(MinecraftServer server) {
        tickLaunchCharges(server);
        tickBursts(server);
        tickWaterRunning(server);
    }

    private static void applyMode(ServerPlayerEntity player, Mode mode) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

        if (speed == null)
            return;

        speed.removeModifier(TIME_SHIFT_SPEED_UUID);

        EntityStepHeightAccessor stepAccessor = (EntityStepHeightAccessor) player;

        if (mode == Mode.OFF) {
            stepAccessor.timeLord$setStepHeight(NORMAL_STEP_HEIGHT);

            sendModeToClient(player, mode);

            player.sendMessage(Text.literal("Time Shift: OFF"), true);

            playModeEffect(player);
            return;
        }

        stepAccessor.timeLord$setStepHeight(TIME_SHIFT_STEP_HEIGHT);

        double modifierAmount = mode.multiplier() - 1.0D;

        EntityAttributeModifier modifier = new EntityAttributeModifier(TIME_SHIFT_SPEED_UUID, "Time Shift Speed",
                modifierAmount, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

        speed.addTemporaryModifier(modifier);
        sendModeToClient(player, mode);
        player.sendMessage(Text.literal("Time Shift: " + mode.multiplier() + "x"), true);
        playModeEffect(player);
    }

    private static void activateBurst(ServerPlayerEntity player, float charge) {
        EntityAttributeInstance speedAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

        if (speedAttribute == null)
            return;

        speedAttribute.removeModifier(TIME_SHIFT_BURST_UUID);

        EntityAttributeModifier burstModifier = new EntityAttributeModifier(TIME_SHIFT_BURST_UUID, "Time Shift Burst",
                BURST_MULTIPLIER - 1.0D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

        speedAttribute.addTemporaryModifier(burstModifier);

        ACTIVE_BURSTS.put(player.getUuid(), new BurstState(BURST_DURATION_TICKS));

        launchPlayerForward(player, charge);
        playBurstEffect(player);
        sendBurstToClient(player);
    }

    private static void launchPlayerForward(ServerPlayerEntity player, float charge) {
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d horizontal = new Vec3d(look.x, 0.0D, look.z);

        if (horizontal.lengthSquared() < 0.0001D) {
            float yawRadians = (float) Math.toRadians(player.getYaw());
            horizontal = new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        }

        horizontal = horizontal.normalize();

        double launchSpeed = MIN_LAUNCH_SPEED + (MAX_LAUNCH_SPEED - MIN_LAUNCH_SPEED) * charge;

        Vec3d currentVelocity = player.getVelocity();

        player.setVelocity(
                horizontal.x * launchSpeed,
                Math.max(currentVelocity.y, LAUNCH_VERTICAL_BOOST),
                horizontal.z * launchSpeed
        );

        player.velocityModified = true;
    }

    private static void tickBursts(MinecraftServer server) {
        Iterator<Map.Entry<UUID, BurstState>> iterator = ACTIVE_BURSTS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BurstState> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            BurstState state = entry.getValue();

            int remaining = state.ticksRemaining() - 1;

            if (remaining <= 0) {
                EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

                if (speed != null)
                    speed.removeModifier(TIME_SHIFT_BURST_UUID);

                iterator.remove();
                continue;
            }
            entry.setValue(new BurstState(remaining));
        }
    }

    private static void removeBurst(ServerPlayerEntity player) {
        ACTIVE_BURSTS.remove(player.getUuid());

        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

        if (speed != null)
            speed.removeModifier(TIME_SHIFT_BURST_UUID);

    }

    private static void tickWaterRunning(MinecraftServer server) {
        for (Map.Entry<UUID, Mode> entry : PLAYER_MODES.entrySet()) {
            UUID playerId = entry.getKey();
            Mode mode = entry.getValue();

            if (mode.multiplier() < MIN_WATER_RUN_MULTIPLIER) {
                clearWaterState(playerId);
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);

            if (player == null || !player.isAlive() || player.isSpectator()) {
                clearWaterState(playerId);
                continue;
            }

            if (LAUNCH_CHARGES.containsKey(player.getUuid()))
                continue;


            captureLandSpeed(player);

            boolean wasWaterRunning = WATER_RUNNING_PLAYERS.contains(playerId);

            boolean waterRunning = tryWaterRun(player);

            if (waterRunning) {
                WATER_RUNNING_PLAYERS.add(playerId);
            } else {
                /*
                 * If water running has just ended,
                 * clear only the locked water baseline.
                 *
                 * Keep LAST_LAND_SPEED around.
                 */
                if (wasWaterRunning) {
                    WATER_BASELINE_SPEED.remove(playerId);
                }

                WATER_RUNNING_PLAYERS.remove(playerId);
            }
        }
    }

    private static void captureLandSpeed(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (WATER_RUNNING_PLAYERS.contains(playerId))
            return;

        if (!hasMovementInput(player))
            return;

        if (!player.isOnGround())
            return;

        if (isWaterNearFeet(player, LAND_WATER_CHECK_DEPTH))
            return;

        Vec3d velocity = player.getVelocity();

        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (horizontalSpeed < 0.01D)
            return;

        if (ACTIVE_BURSTS.containsKey(playerId))
            return;

        LAST_LAND_SPEED.put(playerId, horizontalSpeed);
    }

    private static boolean tryWaterRun(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        Vec3d velocity = player.getVelocity();
        Vec3d horizontalVelocity = new Vec3d(velocity.x, 0.0D, velocity.z);

        double horizontalSpeedSquared = horizontalVelocity.lengthSquared();
        if (!hasMovementInput(player))
            return false;

        if (horizontalSpeedSquared < WATER_RUN_MIN_SPEED_SQUARED)
            return false;

        if (player.isSubmergedInWater())
            return false;

        WaterSurface surface = findWaterSurface(player);

        if (surface == null)
            return false;

        double targetY = surface.surfaceY() + WATER_SURFACE_OFFSET;
        double playerY = player.getY();
        boolean nearSurface = playerY <= targetY + SURFACE_CATCH_ABOVE
                && playerY >= targetY - SURFACE_CATCH_BELOW;

        if (!nearSurface)
            return false;

        if (velocity.y > 0.0D)
            return false;

        if (!WATER_RUNNING_PLAYERS.contains(playerId)) {
            double baseline = LAST_LAND_SPEED.getOrDefault(playerId, horizontalVelocity.length());

            if (baseline < 0.01D)
                baseline = horizontalVelocity.length();

            WATER_BASELINE_SPEED.put(playerId, baseline);
        }
        player.setSwimming(false);

        Vec3d maintainedHorizontal = calculateWaterVelocity(player, horizontalVelocity);

        player.setPosition(player.getX(), targetY, player.getZ());
        player.setVelocity(maintainedHorizontal.x, 0.0D, maintainedHorizontal.z);
        player.fallDistance = 0.0F;
        player.velocityModified = true;

        spawnWaterRunParticles(player, surface.surfaceY(), maintainedHorizontal);

        return true;
    }

    private static Vec3d calculateWaterVelocity(ServerPlayerEntity player, Vec3d horizontalVelocity) {
        UUID playerId = player.getUuid();

        double currentSpeed = horizontalVelocity.length();

        if (currentSpeed < 0.001D)
            return horizontalVelocity;

        double baseline = WATER_BASELINE_SPEED.getOrDefault(playerId, currentSpeed);
        double targetSpeed = baseline;

        if (ACTIVE_BURSTS.containsKey(playerId))
            targetSpeed = Math.max(baseline, currentSpeed);

        return horizontalVelocity.normalize().multiply(targetSpeed);
    }

    private static boolean hasMovementInput(ServerPlayerEntity player) {
        return Math.abs(player.forwardSpeed) > 0.001F || Math.abs(player.sidewaysSpeed) > 0.001F;
    }

    private static WaterSurface findWaterSurface(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        final double[] offsets = {
                0.01D,
                0.10D,
                0.20D,
                0.35D,
                0.50D,
                0.75D
        };

        for (double offset : offsets) {
            if (offset > MAX_WATER_SEARCH_DEPTH)
                break;

            BlockPos pos = BlockPos.ofFloored(player.getX(), player.getY() - offset, player.getZ());

            if (!world.getFluidState(pos).isIn(FluidTags.WATER))
                continue;

            double surfaceY = pos.getY() + 1.0D;

            return new WaterSurface(pos, surfaceY);
        }

        return null;
    }

    private static boolean isWaterNearFeet(ServerPlayerEntity player, double maxDepth) {
        ServerWorld world = player.getServerWorld();

        final double[] offsets = {
                0.01D,
                0.05D,
                0.10D,
                0.15D,
                0.20D
        };

        for (double offset : offsets) {
            if (offset > maxDepth)
                break;

            BlockPos pos = BlockPos.ofFloored(player.getX(), player.getY() - offset, player.getZ());

            if (world.getFluidState(pos).isIn(FluidTags.WATER))
                return true;
        }

        return false;
    }

    private static void spawnWaterRunParticles(ServerPlayerEntity player, double surfaceY, Vec3d horizontalVelocity) {
        ServerWorld world = player.getServerWorld();

        if (player.age % 2 != 0)
            return;

        Vec3d direction = horizontalVelocity.lengthSquared() > 0.001D ? horizontalVelocity.normalize() : Vec3d.ZERO;
        Vec3d particlePosition = new Vec3d(player.getX(), surfaceY + 0.02D, player.getZ());

        /*
         * Put particles behind the player.
         */
        particlePosition = particlePosition.add(direction.multiply(-0.35D));
        int particleCount = getMultiplier(player) >= 10 ? 6 : 4;

        world.spawnParticles(
                ParticleTypes.SPLASH,

                particlePosition.x,
                particlePosition.y,
                particlePosition.z,

                particleCount,

                0.25D,
                0.02D,
                0.25D,

                0.07D
        );
    }

    private static void clearWaterState(UUID playerId) {
        LAST_LAND_SPEED.remove(playerId);
        WATER_BASELINE_SPEED.remove(playerId);
        WATER_RUNNING_PLAYERS.remove(playerId);
    }

    private static void playModeEffect(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        world.spawnParticles(
                ParticleTypes.END_ROD,

                player.getX(),
                player.getBodyY(
                        0.5D
                ),
                player.getZ(),

                40,

                0.5D,
                0.9D,
                0.5D,

                0.08D
        );

        world.playSound(
                null,

                player.getBlockPos(),

                SoundEvents.ENTITY_ENDERMAN_TELEPORT,

                SoundCategory.PLAYERS,

                0.8F,
                1.6F
        );
    }

    private static void playBurstEffect(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        world.playSound(
                null,
                player.getBlockPos(),
                ModSounds.TIME_SHIFT_BOOM,
                SoundCategory.PLAYERS,
                2.0F,
                1.15F
        );

        world.spawnParticles(
                ParticleTypes.ELECTRIC_SPARK,

                player.getX(),
                player.getBodyY(0.5D),
                player.getZ(),

                40,

                0.9D,
                1.0D,
                0.9D,

                0.18D
        );
    }

    private static void sendModeToClient(ServerPlayerEntity player, Mode mode) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBoolean(mode != Mode.OFF);
        buf.writeInt(mode.multiplier());

        ServerPlayNetworking.send(player, TimeLord.TIME_SHIFT_STATE_PACKET, buf);
    }

    private static void sendBurstToClient(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, TimeLord.TIME_SHIFT_BURST_PACKET, PacketByteBufs.empty());
    }

    // PUBLIC HELPERS

    public static boolean isActive(ServerPlayerEntity player) {
        return PLAYER_MODES.getOrDefault(player.getUuid(), Mode.OFF) != Mode.OFF;
    }

    public static int getMultiplier(ServerPlayerEntity player) {
        return PLAYER_MODES.getOrDefault(player.getUuid(), Mode.OFF).multiplier();
    }

    private record BurstState(int ticksRemaining) {}

    private record WaterSurface(BlockPos blockPos, double surfaceY) {}

}