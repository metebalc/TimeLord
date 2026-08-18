package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemporalActionCadenceTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void attackIntervalExpandsInInverseProportionToTemporalScale() {
        assertEquals(5, TemporalActionCadence.attackIntervalTicks(
                5.0D, TemporalState.running(1.0D)));
        assertEquals(10, TemporalActionCadence.attackIntervalTicks(
                5.0D, TemporalState.running(0.5D)));
        assertEquals(34, TemporalActionCadence.attackIntervalTicks(
                5.0D, TemporalState.running(0.15D)));
    }

    @Test
    void personalAccelerationDoesNotBlindlyAccelerateCombatOrItemUse() {
        assertEquals(1.0D,
                TemporalActionCadence.actionScale(TemporalState.running(10.0D)), EPSILON);
    }

    @Test
    void stoppedTimeHasNoActionPermits() {
        assertEquals(0.0D, TemporalActionCadence.actionScale(TemporalState.stopped()), EPSILON);
        assertEquals(Integer.MAX_VALUE,
                TemporalActionCadence.attackIntervalTicks(5.0D, TemporalState.stopped()));
    }
}
