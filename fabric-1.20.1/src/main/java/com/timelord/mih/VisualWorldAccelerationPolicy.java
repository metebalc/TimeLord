package com.timelord.mih;

/** Pure presentation policy. None of these values affect authoritative world simulation. */
public final class VisualWorldAccelerationPolicy {
    public static final double MAX_PARTICLE_TICK_SCALE = 4.0D;

    private VisualWorldAccelerationPolicy() {}

    public static int visualRendererTicks(int rendererTicks, double visualOffsetTicks) {
        if (!Double.isFinite(visualOffsetTicks) || visualOffsetTicks <= 0.0D)
            return rendererTicks;
        return (int) (rendererTicks + (long) Math.floor(visualOffsetTicks));
    }

    public static float visualTickDelta(float tickDelta, double visualFactor) {
        if (!Float.isFinite(tickDelta) || !Double.isFinite(visualFactor))
            return tickDelta;
        return (float) (tickDelta * Math.max(1.0D, visualFactor));
    }

    /**
     * Particles use a compressed, capped scale. Driving short-lived particles at the
     * full 35x celestial factor would erase them in one client tick and multiply cost.
     */
    public static double particleTickScale(double visualFactor) {
        if (!Double.isFinite(visualFactor) || visualFactor <= 1.0D)
            return 1.0D;
        return Math.min(MAX_PARTICLE_TICK_SCALE, 1.0D + Math.sqrt(visualFactor - 1.0D));
    }
}
