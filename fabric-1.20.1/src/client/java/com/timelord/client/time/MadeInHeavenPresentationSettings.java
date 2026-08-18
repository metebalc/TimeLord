package com.timelord.client.time;

import com.timelord.mih.MadeInHeavenPresentationPolicy;
import net.minecraft.client.MinecraftClient;

/** Uses Minecraft's Distortion Effects accessibility option for MIH visuals. */
public final class MadeInHeavenPresentationSettings {
    private MadeInHeavenPresentationSettings() {}

    public static double intensity() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null)
            return 1.0D;
        return client.options.getDistortionEffectScale().getValue();
    }

    public static double visualFactor(double authoritativeFactor) {
        return MadeInHeavenPresentationPolicy.visualFactor(authoritativeFactor, intensity());
    }

    public static double visualOffset(double authoritativeOffset) {
        return MadeInHeavenPresentationPolicy.visualOffset(authoritativeOffset, intensity());
    }

    public static float echoAlpha(float authoritativeAlpha) {
        return MadeInHeavenPresentationPolicy.echoAlpha(authoritativeAlpha, intensity());
    }

    public static float cinematicAlpha(float authoritativeAlpha) {
        return MadeInHeavenPresentationPolicy.cinematicAlpha(authoritativeAlpha, intensity());
    }

    public static boolean allowTemporalSkipping() {
        return MadeInHeavenPresentationPolicy.allowTemporalSkipping(intensity());
    }
}
