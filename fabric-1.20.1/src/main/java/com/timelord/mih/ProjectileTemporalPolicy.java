package com.timelord.mih;

/** Pure policy for projectile cadence that is physically affected only by local Slow Time. */
public final class ProjectileTemporalPolicy {
    private ProjectileTemporalPolicy() {}

    public static double scale(double localSlowTimeFactor) {
        double slow = clamp(localSlowTimeFactor, 0.01D, 1.0D);
        // MIH ownership and progress are presentation inputs only. Vanilla projectile
        // movement stays authoritative; explicit Slow Time fields may still slow it.
        return slow;
    }

    /** Interpolates only after both historical endpoints are known. */
    public static double historicalInterpolationProgress(
            long currentTick,
            long newestSampleTick,
            long previousSampleTick,
            double tickDelta
    ) {
        long duration = Math.max(1L, newestSampleTick - previousSampleTick);
        return clamp(
                (currentTick - newestSampleTick + tickDelta) / duration,
                0.0D,
                1.0D
        );
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value))
            return maximum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
