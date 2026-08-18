package com.timelord.mih;

/** Pure client presentation timeline triggered by an authoritative reset generation. */
public final class MadeInHeavenCinematicTimeline {
    public static final int PRELUDE_START_TICK = 55 * 20;
    public static final int FLASH_DURATION_TICKS = 20;

    private long lastResetGeneration = Long.MIN_VALUE;
    private int flashTicksRemaining;

    public boolean observe(long generationId, MadeInHeavenState.Phase phase) {
        if (phase != MadeInHeavenState.Phase.RESETTING
                || generationId == lastResetGeneration)
            return false;
        lastResetGeneration = generationId;
        flashTicksRemaining = FLASH_DURATION_TICKS;
        return true;
    }

    public void tick() {
        if (flashTicksRemaining > 0)
            flashTicksRemaining--;
    }

    public float flashAlpha(float tickDelta) {
        if (flashTicksRemaining <= 0)
            return 0.0F;
        double elapsed = FLASH_DURATION_TICKS - flashTicksRemaining
                + clamp(tickDelta, 0.0D, 1.0D);
        double progress = elapsed / FLASH_DURATION_TICKS;
        if (progress < 0.2D)
            return (float) lerp(0.35D, 1.0D, MadeInHeavenCurves.smootherstep(progress / 0.2D));
        if (progress < 0.35D)
            return 1.0F;
        return (float) (1.0D - MadeInHeavenCurves.smootherstep((progress - 0.35D) / 0.65D));
    }

    public static float preludeAlpha(int elapsedTicks, boolean adaptedViewer) {
        double progress = (elapsedTicks - PRELUDE_START_TICK)
                / (double) (MadeInHeavenCurves.BUILDUP_TICKS - PRELUDE_START_TICK);
        double maximum = adaptedViewer ? 0.18D : 0.30D;
        return (float) (maximum * MadeInHeavenCurves.smootherstep(progress));
    }

    public boolean isFlashing() {
        return flashTicksRemaining > 0;
    }

    public void clear() {
        lastResetGeneration = Long.MIN_VALUE;
        flashTicksRemaining = 0;
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
