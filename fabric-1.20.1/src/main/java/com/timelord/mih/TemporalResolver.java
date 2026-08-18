package com.timelord.mih;

/**
 * Deterministic priority rules for the currently implemented temporal abilities.
 */
public final class TemporalResolver {
    private TemporalResolver() {}

    /**
     * Resolves both the conceptual player frame and how that frame is enforced.
     * Normal time keeps complete player ticks for networking/input responsiveness.
     * The World alone may stop or fractionally admit whole player ticks.
     */
    public static PlayerResolution resolvePlayerPolicy(PlayerContext context) {
        TemporalState state = resolvePlayer(context);
        double timeShiftMultiplier = clamp(context.timeShiftMultiplier(), 1.0D, 20.0D);

        double wholeEntityTickScale = context.theWorldActive()
                ? (state.isStopped() ? 0.0D : Math.min(1.0D, state.scale()))
                : 1.0D;

        // During The World, fractional whole-entity ticks already express the resistant
        // player's temporal rate. Applying state.scale() to movement as well would square
        // the resistance. The attribute factor only neutralizes raw Time Shift speed.
        double movementAttributeFactor = context.theWorldActive()
                ? 1.0D / timeShiftMultiplier
                : state.scale() / timeShiftMultiplier;

        return new PlayerResolution(
                state,
                clamp(movementAttributeFactor, 0.01D, 1.0D),
                wholeEntityTickScale
        );
    }

    public static TemporalState resolvePlayer(PlayerContext context) {
        if (context.theWorldActive()) {
            if (context.theWorldUser())
                return TemporalState.running(1.0D);
            if (context.madeInHeavenUser()) {
                double resistance = MadeInHeavenCurves.theWorldResistance(
                        context.madeInHeavenElapsedTicks());
                return resistance > 0.0D ? TemporalState.running(resistance) : TemporalState.stopped();
            }
            return TemporalState.stopped();
        }

        double timeShiftScale = clamp(context.timeShiftMultiplier(), 1.0D, 20.0D);
        double slowTimeScale = clamp(context.slowTimeFactor(), 0.01D, 1.0D);
        double combinedScale = timeShiftScale * slowTimeScale;

        // Made in Heaven is perceptual in ordinary time and never changes authoritative
        // player movement or action cadence. Slow Time remains a physical opposing effect.
        if (slowTimeScale < 0.999999D)
            combinedScale = Math.min(1.0D, combinedScale);

        return TemporalState.running(combinedScale);
    }

    public record PlayerContext(
            boolean theWorldActive,
            boolean theWorldUser,
            boolean madeInHeavenActive,
            boolean madeInHeavenUser,
            int madeInHeavenElapsedTicks,
            double timeShiftMultiplier,
            double slowTimeFactor
    ) {
        public PlayerContext {
            madeInHeavenElapsedTicks = Math.max(0, madeInHeavenElapsedTicks);
        }
    }

    public record PlayerResolution(
            TemporalState state,
            double movementAttributeFactor,
            double wholeEntityTickScale
    ) {
        public PlayerResolution {
            if (state == null)
                throw new IllegalArgumentException("state cannot be null");
            if (!Double.isFinite(movementAttributeFactor)
                    || movementAttributeFactor <= 0.0D
                    || movementAttributeFactor > 1.0D)
                throw new IllegalArgumentException("movementAttributeFactor must be in (0, 1]");
            if (!Double.isFinite(wholeEntityTickScale)
                    || wholeEntityTickScale < 0.0D
                    || wholeEntityTickScale > 1.0D)
                throw new IllegalArgumentException("wholeEntityTickScale must be in [0, 1]");
        }

        public boolean isWholeEntityTickStopped() {
            return wholeEntityTickScale <= 0.0D;
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value))
            return maximum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
