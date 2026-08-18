package com.timelord.mih;

/** Bounded presentation policy for historical, non-predictive player echoes. */
public final class TemporalEchoPolicy {
    public static final int MAX_ECHO_COUNT = 3;
    public static final int MAX_SAMPLE_AGE_TICKS = 6;

    private TemporalEchoPolicy() {}

    public static int echoCount(TemporalRenderMode mode, boolean resolvingSkipFrame) {
        return switch (mode) {
            case ACCELERATED -> 1;
            case AFTERIMAGE -> 2;
            case TEMPORAL_SKIP -> resolvingSkipFrame ? 3 : 1;
            default -> 0;
        };
    }

    public static int desiredSampleAgeTicks(int echoIndex) {
        if (echoIndex < 0 || echoIndex >= MAX_ECHO_COUNT)
            throw new IndexOutOfBoundsException("Invalid temporal echo index: " + echoIndex);
        return echoIndex + 1;
    }

    public static float alpha(int echoIndex, TemporalRenderMode mode, boolean resolvingSkipFrame) {
        float base = switch (mode) {
            case ACCELERATED -> 0.18F;
            case AFTERIMAGE -> 0.28F;
            case TEMPORAL_SKIP -> resolvingSkipFrame ? 0.32F : 0.12F;
            default -> 0.0F;
        };
        return Math.max(0.06F, base - echoIndex * 0.08F);
    }
}
