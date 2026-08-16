package com.timelord.hook;

import com.timelord.ability.TheWorldAbility;
import com.timelord.time.TimeController;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.tick.WorldTickScheduler;

/**
 * Minecraft 1.20.1 server-world behavior invoked by the thin world mixin.
 */
public final class ServerWorldHooks {
    private ServerWorldHooks() {
    }

    public static boolean shouldTickEntity(ServerWorld world, Entity entity) {
        return TimeController.shouldTickEntity(world, entity);
    }

    public static boolean shouldRunScheduledTicks(
            ServerWorld world,
            WorldTickScheduler<?> scheduler
    ) {
        if (TheWorldAbility.isTimeStopped() && scheduler == world.getFluidTickScheduler()) {
            return false;
        }

        return scheduler != world.getBlockTickScheduler();
    }
}
