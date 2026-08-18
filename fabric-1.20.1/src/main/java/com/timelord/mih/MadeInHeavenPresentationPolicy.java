package com.timelord.mih;

/** Pure client-presentation policy derived from authoritative MIH state. */
public final class MadeInHeavenPresentationPolicy {
    private static final double MINIMUM_RESET_CUE = 0.25D;
    private static final double TEMPORAL_SKIP_THRESHOLD = 0.35D;

    private MadeInHeavenPresentationPolicy() {}

    public static double visualFactor(double authoritativeFactor, double intensity) {
        double safeFactor = Math.max(1.0D, authoritativeFactor);
        return 1.0D + (safeFactor - 1.0D) * intensity(intensity);
    }

    public static double visualOffset(double authoritativeOffset, double intensity) {
        return Math.max(0.0D, authoritativeOffset) * intensity(intensity);
    }

    public static float echoAlpha(float authoritativeAlpha, double intensity) {
        return (float) (clamp(authoritativeAlpha, 0.0D, 1.0D) * intensity(intensity));
    }

    public static float cinematicAlpha(float authoritativeAlpha, double intensity) {
        double cue = MINIMUM_RESET_CUE
                + (1.0D - MINIMUM_RESET_CUE) * intensity(intensity);
        return (float) (clamp(authoritativeAlpha, 0.0D, 1.0D) * cue);
    }

    public static boolean allowTemporalSkipping(double intensity) {
        return intensity(intensity) >= TEMPORAL_SKIP_THRESHOLD;
    }

    public static HudState hudState(
            MadeInHeavenState.Phase phase,
            int elapsedTicks,
            boolean adapted,
            double viewerScale,
            boolean theWorldActive,
            boolean theWorldUser
    ) {
        double progress = clamp(
                elapsedTicks / (double) MadeInHeavenCurves.BUILDUP_TICKS,
                0.0D,
                1.0D
        );
        double safeScale = clamp(viewerScale, 0.0D, 1.0D);
        double resistance = adapted
                ? MadeInHeavenCurves.theWorldResistance(elapsedTicks)
                : 0.0D;

        HudMode mode;
        if (phase == MadeInHeavenState.Phase.RESETTING) {
            mode = HudMode.RESETTING;
        } else if (phase == MadeInHeavenState.Phase.COLLAPSING) {
            mode = HudMode.COLLAPSING;
        } else if (theWorldActive) {
            if (theWorldUser)
                mode = HudMode.THE_WORLD_DOMINANT;
            else if (resistance > 0.0D)
                mode = HudMode.RESISTING_THE_WORLD;
            else
                mode = HudMode.FROZEN_BY_THE_WORLD;
        } else {
            mode = adapted ? HudMode.ADAPTED : HudMode.SLOWED;
        }
        return new HudState(mode, progress, safeScale, resistance);
    }

    private static double intensity(double intensity) {
        return clamp(intensity, 0.0D, 1.0D);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value))
            return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum HudMode {
        ADAPTED,
        SLOWED,
        COLLAPSING,
        THE_WORLD_DOMINANT,
        RESISTING_THE_WORLD,
        FROZEN_BY_THE_WORLD,
        RESETTING
    }

    public record HudState(
            HudMode mode,
            double progress,
            double viewerScale,
            double theWorldResistance
    ) {}
}
