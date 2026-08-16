package com.timelord.client.mixin;

import com.timelord.client.hook.LivingAnimationFreezeController;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "getAnimationProgress", at = @At("RETURN"), cancellable = true)
    private void timeLord$freezeAnimationProgress(
            LivingEntity entity,
            float tickDelta,
            CallbackInfoReturnable<Float> cir
    ) {
        Float override = LivingAnimationFreezeController.animationProgressOverride(
                entity,
                cir.getReturnValue()
        );
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
