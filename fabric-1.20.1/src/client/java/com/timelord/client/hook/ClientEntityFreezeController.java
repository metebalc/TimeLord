package com.timelord.client.hook;

import com.timelord.client.mixin.LimbAnimatorAccessor;
import com.timelord.client.time.TheWorldClientState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Captures and restores client entity state around the vanilla 1.20.1 entity tick. */
public final class ClientEntityFreezeController {
    private static final Map<UUID, FrozenEntityState> FROZEN_ENTITIES = new HashMap<>();

    private ClientEntityFreezeController() {
    }

    public static void beforeTick(Entity entity) {
        if (!TheWorldClientState.isTimeStopped()) {
            FROZEN_ENTITIES.clear();
            return;
        }

        if (!shouldFreeze(entity)) {
            FROZEN_ENTITIES.remove(entity.getUuid());
            return;
        }

        FrozenEntityState state = FROZEN_ENTITIES.computeIfAbsent(
                entity.getUuid(),
                ignored -> FrozenEntityState.capture(entity)
        );
        restoreTransform(entity, state);
    }

    public static void afterTick(Entity entity) {
        if (!TheWorldClientState.isTimeStopped() || !shouldFreeze(entity)) {
            return;
        }

        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUuid());
        if (state == null) {
            return;
        }

        restoreTransform(entity, state);

        if (entity instanceof LivingEntity living) {
            living.bodyYaw = state.bodyYaw();
            living.prevBodyYaw = state.bodyYaw();
            living.headYaw = state.headYaw();
            living.prevHeadYaw = state.headYaw();
            living.handSwingProgress = state.handSwingProgress();
            living.lastHandSwingProgress = state.handSwingProgress();
            restoreLimbAnimation(living, state);
        }
    }

    private static boolean shouldFreeze(Entity entity) {
        return !(entity instanceof PlayerEntity player)
                || !TheWorldClientState.canMove(player.getUuid());
    }

    private static void restoreTransform(Entity entity, FrozenEntityState state) {
        Vec3d position = state.position();
        entity.setPosition(position.x, position.y, position.z);
        entity.prevX = position.x;
        entity.prevY = position.y;
        entity.prevZ = position.z;
        entity.setYaw(state.yaw());
        entity.prevYaw = state.yaw();
        entity.setPitch(state.pitch());
        entity.prevPitch = state.pitch();
        entity.setVelocity(Vec3d.ZERO);
        entity.age = state.age();

        if (entity instanceof LivingEntity living) {
            restoreLimbAnimation(living, state);
        }
    }

    private static void restoreLimbAnimation(LivingEntity living, FrozenEntityState state) {
        LimbAnimatorAccessor accessor = (LimbAnimatorAccessor) living.limbAnimator;
        accessor.timeLord$setPrevSpeed(state.limbPrevSpeed());
        accessor.timeLord$setSpeed(state.limbSpeed());
        accessor.timeLord$setPos(state.limbPosition());
    }

    private record FrozenEntityState(
            Vec3d position,
            Vec3d velocity,
            float yaw,
            float pitch,
            float bodyYaw,
            float headYaw,
            float handSwingProgress,
            float limbPrevSpeed,
            float limbSpeed,
            float limbPosition,
            int age
    ) {
        private static FrozenEntityState capture(Entity entity) {
            float bodyYaw = entity.getYaw();
            float headYaw = entity.getYaw();
            float handSwingProgress = 0.0F;
            float limbPrevSpeed = 0.0F;
            float limbSpeed = 0.0F;
            float limbPosition = 0.0F;

            if (entity instanceof LivingEntity living) {
                bodyYaw = living.bodyYaw;
                headYaw = living.headYaw;
                handSwingProgress = living.handSwingProgress;

                LimbAnimatorAccessor accessor = (LimbAnimatorAccessor) living.limbAnimator;
                limbPrevSpeed = accessor.timeLord$getPrevSpeed();
                limbSpeed = accessor.timeLord$getSpeed();
                limbPosition = accessor.timeLord$getPos();
            }

            return new FrozenEntityState(
                    entity.getPos(),
                    entity.getVelocity(),
                    entity.getYaw(),
                    entity.getPitch(),
                    bodyYaw,
                    headYaw,
                    handSwingProgress,
                    limbPrevSpeed,
                    limbSpeed,
                    limbPosition,
                    entity.age
            );
        }
    }
}
