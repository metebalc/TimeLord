package com.timelord.time;

import com.timelord.ability.TheWorldAbility;
import com.timelord.network.TimeFieldNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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
    private static long serverTick;

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

        ACTIVE_FIELDS.clear();
        serverTick = 0L;
    }

    public static void tick(MinecraftServer server) {
        serverTick++;
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
            if (entity instanceof ServerPlayerEntity player)
                return TheWorldAbility.canMove(player);

            return false;
        }

        if (ACTIVE_FIELDS.isEmpty())
            return true;

        int largestInterval = 1;

        for (SlowField field : ACTIVE_FIELDS.values()) {
            if (!field.world().equals(world.getRegistryKey()))
                continue;

            if (field.excludedEntityId() != null && field.excludedEntityId().equals(entity.getUuid()))
                continue;

            if (entity.squaredDistanceTo(field.center()) > field.radius() * field.radius())
                continue;

            int interval = Math.max(1, Math.round(1.0F / field.scale()));
            largestInterval = Math.max(largestInterval, interval);
        }
        return largestInterval == 1 || serverTick % largestInterval == 0L;
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
