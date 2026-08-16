package com.timelord.client.mixin;

import com.timelord.client.time.TheWorldClientState;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final Map<UUID, Float> timeLord$frozenAnimationProgress = new HashMap<>();

    @Inject(method = "getAnimationProgress", at = @At("RETURN"), cancellable = true)
    private void timeLord$freezeAnimationProgress(
            LivingEntity entity,
            float tickDelta,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!TheWorldClientState.isTimeStopped()) {
            timeLord$frozenAnimationProgress.clear();
            return;
        }

        if (entity instanceof PlayerEntity player && TheWorldClientState.canMove(player.getUuid())) {
            timeLord$frozenAnimationProgress.remove(entity.getUuid());
            return;
        }

        float frozenProgress = timeLord$frozenAnimationProgress.computeIfAbsent(
                entity.getUuid(),
                ignored -> cir.getReturnValue()
        );

        cir.setReturnValue(frozenProgress);
    }
}
