package com.timelord.mih;

public enum TemporalRenderMode {
    FROZEN,
    SLOW_MOTION,
    NORMAL,
    ACCELERATED,
    AFTERIMAGE,
    TEMPORAL_SKIP;

    public static TemporalRenderMode select(RelativeTemporalFactor relative, boolean moving) {
        return select(relative, moving ? 0.13D : 0.0D);
    }

    public static TemporalRenderMode select(RelativeTemporalFactor relative, double movementMagnitude) {
        if (relative.relation() != RelativeTemporalFactor.Relation.RUNNING)
            return FROZEN;
        if (!Double.isFinite(movementMagnitude) || movementMagnitude < 0.01D)
            return NORMAL;

        double factor = relative.factor();
        if (factor < 0.85D)
            return SLOW_MOTION;

        // A walking user should leave readable trails while a sprinting or sharply
        // redirecting user reaches temporal skipping. Squaring the normalized movement
        // keeps tiny interpolation noise from creating flashes.
        double normalizedMovement = clamp((movementMagnitude - 0.01D) / 0.12D, 0.0D, 1.0D);
        double movementWeight = normalizedMovement * normalizedMovement;
        double perceivedFactor = 1.0D + Math.max(0.0D, factor - 1.0D) * movementWeight;

        if (perceivedFactor <= 1.5D)
            return NORMAL;
        if (perceivedFactor <= 2.5D)
            return ACCELERATED;
        if (perceivedFactor <= 4.5D)
            return AFTERIMAGE;
        return TEMPORAL_SKIP;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
