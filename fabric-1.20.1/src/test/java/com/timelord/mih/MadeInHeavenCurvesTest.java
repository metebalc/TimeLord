package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MadeInHeavenCurvesTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void physicalCurveHitsEveryRequestedTarget() {
        int[] ticks = {0, 200, 400, 600, 800, 1000, 1200};
        double[] scales = {1.0D, 0.85D, 0.65D, 0.45D, 0.30D, 0.20D, 0.15D};

        for (int i = 0; i < ticks.length; i++)
            assertEquals(scales[i], MadeInHeavenCurves.physicalScale(ticks[i]), EPSILON);
    }

    @Test
    void physicalCurveRemainsSmoothlyMonotoneAndBounded() {
        double previous = MadeInHeavenCurves.physicalScale(0);
        for (int tick = 1; tick <= MadeInHeavenCurves.BUILDUP_TICKS; tick++) {
            double current = MadeInHeavenCurves.physicalScale(tick);
            assertTrue(current <= previous + EPSILON, "Scale increased at tick " + tick);
            assertTrue(current >= 0.15D && current <= 1.0D);
            previous = current;
        }
    }

    @Test
    void visualCurveHitsTargetsAndRemainsMonotone() {
        int[] ticks = {0, 200, 400, 600, 800, 1000, 1200};
        double[] factors = {1.0D, 3.0D, 12.0D, 45.0D, 100.0D, 100.0D, 100.0D};

        for (int i = 0; i < ticks.length; i++)
            assertEquals(factors[i], MadeInHeavenCurves.visualWorldFactor(ticks[i]), EPSILON);

        double previous = 1.0D;
        for (int tick = 1; tick <= MadeInHeavenCurves.BUILDUP_TICKS; tick++) {
            double current = MadeInHeavenCurves.visualWorldFactor(tick);
            assertTrue(current + EPSILON >= previous, "Visual factor decreased at tick " + tick);
            previous = current;
        }
    }

    @Test
    void resistanceKeepsTheWorldDominant() {
        assertEquals(0.0D, MadeInHeavenCurves.theWorldResistance(400), EPSILON);
        assertTrue(MadeInHeavenCurves.theWorldResistance(600) > 0.0D);
        assertEquals(0.03D, MadeInHeavenCurves.theWorldResistance(800), EPSILON);
        assertEquals(0.08D, MadeInHeavenCurves.theWorldResistance(1200), EPSILON);
    }

    @Test
    void visualWorldOffsetIsDeterministicAndCollapsesToServerTime() {
        double halfway = MadeInHeavenCurves.visualWorldOffsetTicks(600);
        double maximum = MadeInHeavenCurves.visualWorldOffsetTicks(1200);

        assertTrue(halfway > 0.0D);
        assertTrue(maximum > halfway);
        assertEquals(maximum,
                MadeInHeavenCurves.collapseVisualWorldOffset(maximum, 0.0D), EPSILON);
        assertEquals(0.0D,
                MadeInHeavenCurves.collapseVisualWorldOffset(maximum, 1.0D), EPSILON);
    }
}
