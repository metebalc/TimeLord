package com.timelord.client.mixin;

import com.timelord.client.time.TheWorldClientState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Unique
    private static final Map<UUID, FrozenEntityState> timeLord$frozenEntities =
            new HashMap<>();

    @Inject(
            method = "tickEntity",
            at = @At("HEAD")
    )
    private void timeLord$freezeEntityHead(
            Entity entity,
            CallbackInfo ci
    ) {
        if (!TheWorldClientState.isTimeStopped()) {
            timeLord$frozenEntities.clear();
            return;
        }

        if (!timeLord$shouldFreeze(entity)) {
            timeLord$frozenEntities.remove(
                    entity.getUuid()
            );

            return;
        }

        FrozenEntityState state =
                timeLord$frozenEntities.computeIfAbsent(
                        entity.getUuid(),
                        ignored ->
                                FrozenEntityState.capture(
                                        entity
                                )
                );

        timeLord$restoreTransform(
                entity,
                state
        );
    }

    @Inject(
            method = "tickEntity",
            at = @At("TAIL")
    )
    private void timeLord$freezeEntityTail(
            Entity entity,
            CallbackInfo ci
    ) {
        if (!TheWorldClientState.isTimeStopped())
            return;

        if (!timeLord$shouldFreeze(entity))
            return;

        FrozenEntityState state =
                timeLord$frozenEntities.get(
                        entity.getUuid()
                );

        if (state == null)
            return;

        timeLord$restoreTransform(
                entity,
                state
        );

        if (entity instanceof LivingEntity living) {
            living.bodyYaw =
                    state.bodyYaw();

            living.prevBodyYaw =
                    state.bodyYaw();

            living.headYaw =
                    state.headYaw();

            living.prevHeadYaw =
                    state.headYaw();

            living.handSwingProgress =
                    state.handSwingProgress();

            living.lastHandSwingProgress =
                    state.handSwingProgress();

            timeLord$restoreLimbAnimation(living, state);
        }
    }

    @Unique
    private static boolean timeLord$shouldFreeze(
            Entity entity
    ) {
        if (!(entity instanceof PlayerEntity player)) {
            return true;
        }

        return !TheWorldClientState.canMove(
                player.getUuid()
        );
    }

    @Unique
    private static void timeLord$restoreTransform(
            Entity entity,
            FrozenEntityState state
    ) {
        Vec3d position =
                state.position();

        entity.setPosition(
                position.x,
                position.y,
                position.z
        );

        entity.prevX =
                position.x;

        entity.prevY =
                position.y;

        entity.prevZ =
                position.z;

        entity.setYaw(
                state.yaw()
        );

        entity.prevYaw =
                state.yaw();

        entity.setPitch(
                state.pitch()
        );

        entity.prevPitch =
                state.pitch();

        entity.setVelocity(
                Vec3d.ZERO
        );

        entity.age =
                state.age();

        if (entity instanceof LivingEntity living)
            timeLord$restoreLimbAnimation(living, state);
    }

    @Unique
    private static void timeLord$restoreLimbAnimation(
            LivingEntity living,
            FrozenEntityState state
    ) {
        LimbAnimatorAccessor accessor =
                (LimbAnimatorAccessor) living.limbAnimator;

        accessor.timeLord$setPrevSpeed(state.limbPrevSpeed());
        accessor.timeLord$setSpeed(state.limbSpeed());
        accessor.timeLord$setPos(state.limbPosition());
    }

    @Unique
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

        private static FrozenEntityState capture(
                Entity entity
        ) {
            float bodyYaw =
                    entity.getYaw();

            float headYaw =
                    entity.getYaw();

            float handSwingProgress =
                    0.0F;

            float limbPrevSpeed =
                    0.0F;

            float limbSpeed =
                    0.0F;

            float limbPosition =
                    0.0F;

            if (entity instanceof LivingEntity living) {
                bodyYaw =
                        living.bodyYaw;

                headYaw =
                        living.headYaw;

                handSwingProgress =
                        living.handSwingProgress;

                LimbAnimatorAccessor accessor =
                        (LimbAnimatorAccessor) living.limbAnimator;

                limbPrevSpeed =
                        accessor.timeLord$getPrevSpeed();

                limbSpeed =
                        accessor.timeLord$getSpeed();

                limbPosition =
                        accessor.timeLord$getPos();
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
