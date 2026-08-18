package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemporalRenderModeTest {
    private static final RelativeTemporalFactor MAXIMUM_RELATIVE_SPEED =
            RelativeTemporalFactor.between(
                    TemporalState.running(0.15D),
                    TemporalState.running(1.0D)
            );

    @Test
    void visibilityDependsOnActualMovementMagnitude() {
        assertEquals(TemporalRenderMode.NORMAL,
                TemporalRenderMode.select(MAXIMUM_RELATIVE_SPEED, 0.0D));
        assertEquals(TemporalRenderMode.AFTERIMAGE,
                TemporalRenderMode.select(MAXIMUM_RELATIVE_SPEED, 0.08D));
        assertEquals(TemporalRenderMode.TEMPORAL_SKIP,
                TemporalRenderMode.select(MAXIMUM_RELATIVE_SPEED, 0.13D));
    }

    @Test
    void slowRelativeMotionRemainsSmoothRatherThanSkipping() {
        RelativeTemporalFactor slow = RelativeTemporalFactor.between(
                TemporalState.running(1.0D),
                TemporalState.running(0.2D)
        );
        assertEquals(TemporalRenderMode.SLOW_MOTION,
                TemporalRenderMode.select(slow, 0.13D));
    }
}
