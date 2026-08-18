package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualWorldAccelerationPolicyTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void rendererClockUsesOnlyTheDeterministicVisualOffset() {
        assertEquals(120, VisualWorldAccelerationPolicy.visualRendererTicks(100, 20.9D));
        assertEquals(100, VisualWorldAccelerationPolicy.visualRendererTicks(100, -1.0D));
        assertEquals(17.5F, VisualWorldAccelerationPolicy.visualTickDelta(0.5F, 35.0D));
    }

    @Test
    void particleScaleIsCompressedAndCapped() {
        assertEquals(1.0D, VisualWorldAccelerationPolicy.particleTickScale(1.0D), EPSILON);
        assertTrue(VisualWorldAccelerationPolicy.particleTickScale(3.0D) > 1.0D);
        assertEquals(4.0D, VisualWorldAccelerationPolicy.particleTickScale(35.0D), EPSILON);
    }

    @Test
    void fractionalParticleTicksRemainDeterministic() {
        VisualTickAccumulator accumulator = new VisualTickAccumulator();
        int total = 0;
        for (int i = 0; i < 4; i++)
            total += accumulator.advance(1.5D);
        assertEquals(6, total);

        accumulator.reset();
        assertEquals(1, accumulator.advance(1.0D));
    }
}
