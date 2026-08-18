package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalEchoPolicyTest {
    @Test
    void echoesRemainStrictlyBounded() {
        assertEquals(0, TemporalEchoPolicy.echoCount(TemporalRenderMode.NORMAL, true));
        assertEquals(1, TemporalEchoPolicy.echoCount(TemporalRenderMode.ACCELERATED, true));
        assertEquals(2, TemporalEchoPolicy.echoCount(TemporalRenderMode.AFTERIMAGE, true));
        assertEquals(3, TemporalEchoPolicy.echoCount(TemporalRenderMode.TEMPORAL_SKIP, true));
        assertEquals(1, TemporalEchoPolicy.echoCount(TemporalRenderMode.TEMPORAL_SKIP, false));
    }

    @Test
    void samplesUseRecentHistoryAndFadeWithAge() {
        assertEquals(1, TemporalEchoPolicy.desiredSampleAgeTicks(0));
        assertEquals(3, TemporalEchoPolicy.desiredSampleAgeTicks(2));
        assertTrue(TemporalEchoPolicy.alpha(0, TemporalRenderMode.AFTERIMAGE, true)
                > TemporalEchoPolicy.alpha(1, TemporalRenderMode.AFTERIMAGE, true));
    }
}
