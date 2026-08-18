package com.timelord.mih;

/** Pure policy for translating a resolved player frame into server action cadence. */
public final class TemporalActionCadence {
    private TemporalActionCadence() {}

    public static int attackIntervalTicks(double normalAttackIntervalTicks, TemporalState state) {
        if (state.isStopped())
            return Integer.MAX_VALUE;

        double safeInterval = Double.isFinite(normalAttackIntervalTicks)
                ? Math.max(1.0D, normalAttackIntervalTicks)
                : 1.0D;
        double actionScale = actionScale(state);
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(safeInterval / actionScale));
    }

    public static double actionScale(TemporalState state) {
        if (state.isStopped())
            return 0.0D;
        return Math.min(1.0D, state.scale());
    }
}
