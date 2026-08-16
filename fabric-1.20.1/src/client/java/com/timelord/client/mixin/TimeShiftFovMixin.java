package com.timelord.client.mixin;

import com.timelord.client.hook.TimeShiftFovHook;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class TimeShiftFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void timeLord$modifyTimeShiftFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        Double override = TimeShiftFovHook.override(cir.getReturnValue());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
