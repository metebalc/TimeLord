package com.timelord.mih;

/** Pure cadence policy for resolving real samples at extreme relative speed. */
public final class TemporalSkipCadence {
    private TemporalSkipCadence() {}

    public static int intervalTicks(double relativeFactor) {
        if (!Double.isFinite(relativeFactor) || relativeFactor < 3.75D)
            return 1;
        if (relativeFactor < 5.5D)
            return 2;
        return 3;
    }

    public static boolean shouldResolve(long clientTick, int stablePhase, double relativeFactor) {
        int interval = intervalTicks(relativeFactor);
        return interval == 1 || Math.floorMod(clientTick + stablePhase, interval) == 0;
    }
}
