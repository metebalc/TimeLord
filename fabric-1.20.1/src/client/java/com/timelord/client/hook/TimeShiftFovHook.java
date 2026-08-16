package com.timelord.client.hook;

import com.timelord.client.TimeLordClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/** Computes the Time Shift FOV adjustment outside the version-sensitive mixin. */
public final class TimeShiftFovHook {
    private static final float X2_MAX_FOV_BOOST = 8.0F;
    private static final float X3_MAX_FOV_BOOST = 12.0F;
    private static final float X5_MAX_FOV_BOOST = 18.0F;
    private static final float X10_MAX_FOV_BOOST = 25.0F;
    private static final float BURST_FOV_BOOST = 14.0F;

    private TimeShiftFovHook() {
    }

    public static Double override(double currentFov) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return null;
        }

        int multiplier = TimeLordClient.getTimeShiftMultiplier();
        if (multiplier <= 0) {
            return null;
        }

        double fov = currentFov;
        if (TimeLordClient.isTimeShiftKeyDown()) {
            float progress = TimeLordClient.getTimeShiftHoldProgress();
            float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
            fov += MathHelper.lerp(smoothProgress, 0.0F, maximumBoost(multiplier));
        }

        if (TimeLordClient.isTimeShiftBursting()) {
            fov += BURST_FOV_BOOST;
        }

        return fov;
    }

    private static float maximumBoost(int multiplier) {
        if (multiplier >= 10) {
            return X10_MAX_FOV_BOOST;
        }
        if (multiplier >= 5) {
            return X5_MAX_FOV_BOOST;
        }
        if (multiplier >= 3) {
            return X3_MAX_FOV_BOOST;
        }
        return X2_MAX_FOV_BOOST;
    }
}
