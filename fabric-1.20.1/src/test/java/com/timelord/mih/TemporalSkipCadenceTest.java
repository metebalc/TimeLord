package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalSkipCadenceTest {
    @Test
    void thresholdsIncreaseSkippedTicksWithoutDroppingEveryFrame() {
        assertEquals(1, TemporalSkipCadence.intervalTicks(3.74D));
        assertEquals(2, TemporalSkipCadence.intervalTicks(4.0D));
        assertEquals(3, TemporalSkipCadence.intervalTicks(6.67D));
    }

    @Test
    void maximumCadenceStillResolvesRealFramesRegularly() {
        int resolved = 0;
        for (int tick = 0; tick < 30; tick++) {
            if (TemporalSkipCadence.shouldResolve(tick, 0, 6.67D))
                resolved++;
        }

        assertEquals(10, resolved);
        assertTrue(TemporalSkipCadence.shouldResolve(0, 0, 6.67D));
        assertFalse(TemporalSkipCadence.shouldResolve(1, 0, 6.67D));
    }
}
