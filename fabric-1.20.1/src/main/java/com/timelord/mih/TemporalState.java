package com.timelord.mih;

/**
 * A running temporal frame or the special stopped-time state.
 */
public record TemporalState(Kind kind, double scale) {
    public TemporalState {
        if (kind == null)
            throw new IllegalArgumentException("kind cannot be null");
        if (kind == Kind.RUNNING && (!Double.isFinite(scale) || scale <= 0.0D))
            throw new IllegalArgumentException("A running temporal scale must be finite and positive");
        if (kind == Kind.STOPPED)
            scale = 0.0D;
    }

    public static TemporalState running(double scale) {
        return new TemporalState(Kind.RUNNING, scale);
    }

    public static TemporalState stopped() {
        return new TemporalState(Kind.STOPPED, 0.0D);
    }

    public boolean isStopped() {
        return kind == Kind.STOPPED;
    }

    public enum Kind {
        RUNNING,
        STOPPED
    }
}
