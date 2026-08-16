package com.timelord.client.hook;

import com.timelord.client.time.TheWorldClientState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Holds living-entity animation progress while client time is stopped. */
public final class LivingAnimationFreezeController {
    private static final Map<UUID, Float> FROZEN_PROGRESS = new HashMap<>();

    private LivingAnimationFreezeController() {
    }

    public static Float animationProgressOverride(LivingEntity entity, float currentProgress) {
        if (!TheWorldClientState.isTimeStopped()) {
            FROZEN_PROGRESS.clear();
            return null;
        }

        if (entity instanceof PlayerEntity player
                && TheWorldClientState.canMove(player.getUuid())) {
            FROZEN_PROGRESS.remove(entity.getUuid());
            return null;
        }

        return FROZEN_PROGRESS.computeIfAbsent(entity.getUuid(), ignored -> currentProgress);
    }
}
