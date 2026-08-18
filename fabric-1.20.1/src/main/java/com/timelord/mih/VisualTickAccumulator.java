package com.timelord.mih;

/** Converts a bounded visual scale into a deterministic number of client-only ticks. */
public final class VisualTickAccumulator {
    private double remainder;

    public int advance(double scale) {
        double bounded = Double.isFinite(scale)
                ? Math.max(1.0D, Math.min(VisualWorldAccelerationPolicy.MAX_PARTICLE_TICK_SCALE, scale))
                : 1.0D;
        double total = remainder + bounded;
        int ticks = Math.max(1, (int) Math.floor(total));
        remainder = total - ticks;
        return ticks;
    }

    public void reset() {
        remainder = 0.0D;
    }
}
