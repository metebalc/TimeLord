package com.timelord.mih;

/**
 * Authoritative Made in Heaven progression functions for Minecraft 1.20.1.
 */
public final class MadeInHeavenCurves {
    public static final int BUILDUP_TICKS = 60 * 20;
    public static final int COLLAPSE_TICKS = 4 * 20;
    public static final double MINIMUM_SCALE = 0.15D;

    private static final double[] TICK_KNOTS = {
            0.0D, 200.0D, 400.0D, 600.0D, 800.0D, 1000.0D, 1200.0D
    };
    private static final MonotoneCubicCurve PHYSICAL_SCALE = new MonotoneCubicCurve(
            TICK_KNOTS,
            new double[]{1.0D, 0.85D, 0.65D, 0.45D, 0.30D, 0.20D, 0.15D}
    );
    private static final MonotoneCubicCurve LOG_VISUAL_FACTOR = createLogVisualFactor();
    private static final double[] VISUAL_WORLD_OFFSET_TICKS = createVisualWorldOffsets();

    private MadeInHeavenCurves() {}

    private static MonotoneCubicCurve createLogVisualFactor() {
        double[] visualFactors = {1.0D, 50.0D, 100.0D, 400.0D, 1100.0D, 1600.0D, 2500.0D};
        for (int i = 0; i < visualFactors.length; i++)
            visualFactors[i] = Math.log(visualFactors[i]);
        return new MonotoneCubicCurve(TICK_KNOTS, visualFactors);
    }

    private static double[] createVisualWorldOffsets() {
        double[] offsets = new double[BUILDUP_TICKS + 1];
        for (int tick = 1; tick <= BUILDUP_TICKS; tick++)
            offsets[tick] = offsets[tick - 1] + visualWorldFactor(tick - 1) - 1.0D;
        return offsets;
    }

    public static double physicalScale(double elapsedTicks) {
        return clamp(PHYSICAL_SCALE.value(elapsedTicks), MINIMUM_SCALE, 1.0D);
    }

    public static double visualWorldFactor(double elapsedTicks) {
        return clamp(Math.exp(LOG_VISUAL_FACTOR.value(elapsedTicks)), 1.0D, 2500.0D);
    }

    /** Deterministic visual-only lead over authoritative server time. */
    public static double visualWorldOffsetTicks(int elapsedTicks) {
        int safeTick = Math.max(0, Math.min(BUILDUP_TICKS, elapsedTicks));
        return VISUAL_WORLD_OFFSET_TICKS[safeTick];
    }

    public static double collapseVisualWorldOffset(double startOffsetTicks, double progress) {
        return Math.max(0.0D, startOffsetTicks) * (1.0D - smootherstep(progress));
    }

    public static double collapsePhysicalScale(double startScale, double progress) {
        return lerp(clamp(startScale, 0.0D, 1.0D), 1.0D, smootherstep(progress));
    }

    public static double collapseVisualFactor(double startFactor, double progress) {
        double safeStart = Math.max(1.0D, startFactor);
        return Math.exp(lerp(Math.log(safeStart), 0.0D, smootherstep(progress)));
    }

    public static double theWorldResistance(double elapsedTicks) {
        if (elapsedTicks <= 400.0D)
            return 0.0D;
        if (elapsedTicks <= 800.0D)
            return 0.03D * smootherstep((elapsedTicks - 400.0D) / 400.0D);
        return 0.03D + 0.05D * smootherstep((elapsedTicks - 800.0D) / 400.0D);
    }

    public static double smootherstep(double value) {
        double t = clamp(value, 0.0D, 1.0D);
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
