package com.timelord.common.logic;

import com.timelord.common.model.TemporalPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatMathTest {
    @Test
    void identifiesAProjectileOnACollisionCourse() {
        TemporalPosition player = TemporalPosition.ZERO;

        assertTrue(ThreatMath.isDangerousProjectile(
                player,
                new TemporalPosition(10.0D, 0.0D, 0.0D),
                new TemporalPosition(-1.0D, 0.0D, 0.0D)
        ));

        assertFalse(ThreatMath.isDangerousProjectile(
                player,
                new TemporalPosition(10.0D, 0.0D, 0.0D),
                new TemporalPosition(1.0D, 0.0D, 0.0D)
        ));
    }

    @Test
    void identifiesTargetingAndApproachingHostiles() {
        TemporalPosition player = TemporalPosition.ZERO;
        TemporalPosition hostile = new TemporalPosition(8.0D, 0.0D, 0.0D);

        assertTrue(ThreatMath.isRelevantHostile(player, hostile, TemporalPosition.ZERO, true));
        assertTrue(ThreatMath.isRelevantHostile(
                player,
                hostile,
                new TemporalPosition(-0.2D, 0.0D, 0.0D),
                false
        ));
        assertFalse(ThreatMath.isRelevantHostile(
                player,
                hostile,
                new TemporalPosition(0.2D, 0.0D, 0.0D),
                false
        ));
    }
}
