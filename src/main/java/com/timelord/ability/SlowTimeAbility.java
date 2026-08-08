package com.timelord.ability;

import com.timelord.time.TimeController;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SlowTimeAbility implements Ability {
    private static final float TIME_SCALE = 0.25F;
    private static final double RADIUS = 16.0D;

    private final int durationTicks;

    public SlowTimeAbility(int durationSeconds) {
        this.durationTicks = durationSeconds * 20;
    }

    @Override
    public void activate(ServerPlayerEntity player) {
        TimeController.slowTime(player, TIME_SCALE, durationTicks, RADIUS);
    }

    @Override
    public void deactivate(ServerPlayerEntity player) {
        TimeController.resetTime(player.getUuid());
    }
}
