package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class TimeShiftFovMixin {
    private static final float X2_MAX_FOV_BOOST = 8.0F;
    private static final float X3_MAX_FOV_BOOST = 12.0F;
    private static final float X5_MAX_FOV_BOOST = 18.0F;
    private static final float X10_MAX_FOV_BOOST = 25.0F;

    private static final float BURST_FOV_BOOST = 14.0F;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void timeLord$modifyTimeShiftFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null)
            return;

        int multiplier = TimeLordClient
                        .getTimeShiftMultiplier();

        if (multiplier <= 0)
            return;

        double fov = cir.getReturnValue();

        if (TimeLordClient.isTimeShiftKeyDown()) {
            float progress = TimeLordClient.getTimeShiftHoldProgress();
            float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
            float maxBoost = getMaximumFovBoost(multiplier);
            float chargeEmphasis = MathHelper.lerp(smoothProgress, 0.0F, maxBoost);

            fov += chargeEmphasis;
        }

        if (TimeLordClient.isTimeShiftBursting())
            fov += BURST_FOV_BOOST;

        cir.setReturnValue(fov);
    }

    private static float getMaximumFovBoost(int multiplier) {
        if (multiplier >= 10)
            return X10_MAX_FOV_BOOST;

        if (multiplier >= 5)
            return X5_MAX_FOV_BOOST;

        if (multiplier >= 3)
            return X3_MAX_FOV_BOOST;

        return X2_MAX_FOV_BOOST;
    }

}