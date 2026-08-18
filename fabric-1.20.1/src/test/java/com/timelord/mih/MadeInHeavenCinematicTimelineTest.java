package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MadeInHeavenCinematicTimelineTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void onlyOneFlashIsStartedForEachAuthoritativeGeneration() {
        MadeInHeavenCinematicTimeline timeline = new MadeInHeavenCinematicTimeline();
        assertFalse(timeline.observe(1L, MadeInHeavenState.Phase.BUILDUP));
        assertTrue(timeline.observe(1L, MadeInHeavenState.Phase.RESETTING));
        timeline.tick();
        assertFalse(timeline.observe(1L, MadeInHeavenState.Phase.RESETTING));
        assertTrue(timeline.observe(2L, MadeInHeavenState.Phase.RESETTING));
    }

    @Test
    void flashRisesHoldsAndThenFullyFades() {
        MadeInHeavenCinematicTimeline timeline = new MadeInHeavenCinematicTimeline();
        timeline.observe(4L, MadeInHeavenState.Phase.RESETTING);
        float initial = timeline.flashAlpha(0.0F);
        for (int tick = 0; tick < 5; tick++)
            timeline.tick();
        assertTrue(timeline.flashAlpha(0.0F) > initial);
        for (int tick = 5; tick < MadeInHeavenCinematicTimeline.FLASH_DURATION_TICKS; tick++)
            timeline.tick();
        assertFalse(timeline.isFlashing());
        assertEquals(0.0D, timeline.flashAlpha(0.0F), EPSILON);
    }

    @Test
    void preludeIsBoundedAndAdaptedViewersReceiveLessBleaching() {
        assertEquals(0.0D,
                MadeInHeavenCinematicTimeline.preludeAlpha(1000, false), EPSILON);
        assertEquals(0.30D,
                MadeInHeavenCinematicTimeline.preludeAlpha(1200, false), EPSILON);
        assertEquals(0.18D,
                MadeInHeavenCinematicTimeline.preludeAlpha(1200, true), EPSILON);
    }
}
