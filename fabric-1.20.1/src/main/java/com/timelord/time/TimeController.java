package com.timelord.time;

import com.timelord.ability.TheWorldAbility;
import com.timelord.mih.FractionalTickAccumulator;
import com.timelord.mih.MadeInHeavenPhysicalController;
import com.timelord.mih.ProjectileTemporalPolicy;
import com.timelord.mih.TemporalResolver;
import com.timelord.network.TimeFieldNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TimeController {
    private static final Map<UUID, SlowField> ACTIVE_FIELDS = new HashMap<>();
    private static final FractionalTickAccumulator THE_WORLD_RESISTANCE_TICKS =
            new FractionalTickAccumulator();
    private static final FractionalTickAccumulator SLOW_ENTITY_TICKS =
            new FractionalTickAccumulator();

    private TimeController() {}

    public static void slowTime(ServerPlayerEntity player, float scale, int durationTicks, double radius) {
        float safeScale = Math.max(0.025F, Math.min(1.0F, scale));
        Vec3d center = player.getPos();

        ACTIVE_FIELDS.put(player.getUuid(),
                new SlowField(player.getWorld().getRegistryKey(), center, radius, safeScale, durationTicks, player.getUuid()));

        ServerWorld world = player.getServerWorld();
        TimeFieldNetworking.sendStartField(player.getServer(), world, player.getUuid(), center, radius, durationTicks);
    }

    public static void resetTime(MinecraftServer server, UUID owner) {
        ACTIVE_FIELDS.remove(owner);

        TimeFieldNetworking.sendRemoveField(server, owner);
    }

    public static void resetAll(MinecraftServer server) {
        for (UUID owner : ACTIVE_FIELDS.keySet())
            TimeFieldNetworking.sendRemoveField(server, owner);

        clear();
    }

    public static void clear() {
        ACTIVE_FIELDS.clear();
        THE_WORLD_RESISTANCE_TICKS.clear();
        SLOW_ENTITY_TICKS.clear();
    }

    /** Drops fractional cadence that must not cross player lifecycle boundaries. */
    public static void clearTemporalRemainders(UUID entityId) {
        THE_WORLD_RESISTANCE_TICKS.remove(entityId);
        SLOW_ENTITY_TICKS.remove(entityId);
    }

    /** Clears temporal cadence without removing active Slow Time fields. */
    public static void clearTemporalRemainders() {
        THE_WORLD_RESISTANCE_TICKS.clear();
        SLOW_ENTITY_TICKS.clear();
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, SlowField>> iterator = ACTIVE_FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SlowField> entry = iterator.next();
            SlowField field = entry.getValue();
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(entry.getKey());

            if (owner == null || !owner.getWorld().getRegistryKey().equals(field.world())) {
                TimeFieldNetworking.sendRemoveField(server, entry.getKey());
                iterator.remove();
                continue;
            }

            int ticksLeft = field.remainingTicks() - 1;
            if (ticksLeft <= 0) {
                TimeFieldNetworking.sendRemoveField(server, entry.getKey());
                iterator.remove();
            } else {
                entry.setValue(field.withRemainingTicks(ticksLeft));
            }
        }
    }

    public static boolean shouldTickEntity(ServerWorld world, Entity entity) {
        if (TheWorldAbility.isTimeStopped()) {
            if (entity instanceof ServerPlayerEntity player) {
                TemporalResolver.PlayerResolution resolution =
                        MadeInHeavenPhysicalController.temporalResolution(player);
                double wholeTickScale = resolution.wholeEntityTickScale();
                if (wholeTickScale >= 0.999999D) {
                    THE_WORLD_RESISTANCE_TICKS.remove(player.getUuid());
                    return true;
                }

                if (!resolution.isWholeEntityTickStopped())
                    return THE_WORLD_RESISTANCE_TICKS.shouldStep(
                            player.getUuid(), wholeTickScale);

                THE_WORLD_RESISTANCE_TICKS.remove(player.getUuid());
            }

            return false;
        }

        THE_WORLD_RESISTANCE_TICKS.remove(entity.getUuid());

        double entityTemporalFactor = entityTemporalFactor(world, entity);
        if (entityTemporalFactor >= 0.999999D) {
            SLOW_ENTITY_TICKS.remove(entity.getUuid());
            return true;
        }

        // Player input, networking, and camera-facing state must continue ticking. Their
        // movement and actions are slowed independently by the authoritative player frame.
        if (entity instanceof ServerPlayerEntity) {
            SLOW_ENTITY_TICKS.remove(entity.getUuid());
            return true;
        }

        return SLOW_ENTITY_TICKS.shouldStep(entity.getUuid(), entityTemporalFactor);
    }

    private static double entityTemporalFactor(ServerWorld world, Entity entity) {
        double localSlowTime = slowTimeFactor(world, entity);
        if (!(entity instanceof ProjectileEntity projectile))
            return localSlowTime;

        return ProjectileTemporalPolicy.scale(localSlowTime);
    }

    public static double slowTimeFactor(ServerWorld world, Entity entity) {
        double factor = 1.0D;
        for (SlowField field : ACTIVE_FIELDS.values()) {
            if (!field.world().equals(world.getRegistryKey()))
                continue;
            if (field.excludedEntityId() != null && field.excludedEntityId().equals(entity.getUuid()))
                continue;
            if (entity.squaredDistanceTo(field.center()) > field.radius() * field.radius())
                continue;
            factor = Math.min(factor, field.scale());
        }
        return factor;
    }

    public static boolean isTimeSlowed() {
        return !ACTIVE_FIELDS.isEmpty();
    }

    public static void addTemporaryField(ServerPlayerEntity owner, Vec3d center, double radius, float scale, int durationTicks) {
        float safeScale = Math.max(0.01F, Math.min(1.0F, scale));

        ACTIVE_FIELDS.put(owner.getUuid(),
                new SlowField(
                        owner.getWorld().getRegistryKey(),
                        center,
                        radius,
                        safeScale,
                        durationTicks,
                        owner.getUuid()
                )
        );
    }

    public static void removeTemporaryField(UUID owner) {
        ACTIVE_FIELDS.remove(owner);
    }

    private record SlowField(RegistryKey<World> world, Vec3d center, double radius, float scale, int remainingTicks, UUID excludedEntityId) {
        private SlowField withRemainingTicks(int ticks) {
            return new SlowField(world, center, radius, scale, ticks, excludedEntityId);
        }
    }

}
