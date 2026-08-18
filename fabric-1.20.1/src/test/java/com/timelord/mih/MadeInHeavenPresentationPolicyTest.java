package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MadeInHeavenPresentationPolicyTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void intensityScalesVisualsWithoutChangingTheirAuthoritativeInputs() {
        assertEquals(1.0D, MadeInHeavenPresentationPolicy.visualFactor(35.0D, 0.0D), EPSILON);
        assertEquals(18.0D, MadeInHeavenPresentationPolicy.visualFactor(35.0D, 0.5D), EPSILON);
        assertEquals(35.0D, MadeInHeavenPresentationPolicy.visualFactor(35.0D, 1.0D), EPSILON);
        assertEquals(250.0D, MadeInHeavenPresentationPolicy.visualOffset(500.0D, 0.5D), EPSILON);
    }

    @Test
    void resetCueNeverCompletelyDisappears() {
        assertEquals(0.25D,
                MadeInHeavenPresentationPolicy.cinematicAlpha(1.0F, 0.0D), EPSILON);
        assertEquals(1.0D,
                MadeInHeavenPresentationPolicy.cinematicAlpha(1.0F, 1.0D), EPSILON);
        assertFalse(MadeInHeavenPresentationPolicy.allowTemporalSkipping(0.34D));
        assertTrue(MadeInHeavenPresentationPolicy.allowTemporalSkipping(0.35D));
    }

    @Test
    void hudMakesTheWorldPriorityAndResistanceExplicit() {
        assertEquals(MadeInHeavenPresentationPolicy.HudMode.FROZEN_BY_THE_WORLD,
                hud(400, true, true, false).mode());
        MadeInHeavenPresentationPolicy.HudState resistant =
                hud(1200, true, true, false);
        assertEquals(MadeInHeavenPresentationPolicy.HudMode.RESISTING_THE_WORLD,
                resistant.mode());
        assertEquals(0.08D, resistant.theWorldResistance(), EPSILON);
        assertEquals(MadeInHeavenPresentationPolicy.HudMode.THE_WORLD_DOMINANT,
                hud(1200, true, true, true).mode());
    }

    @Test
    void normalAndAdaptedViewersReceiveDifferentHudIdentity() {
        assertEquals(MadeInHeavenPresentationPolicy.HudMode.ADAPTED,
                hud(600, true, false, false).mode());
        MadeInHeavenPresentationPolicy.HudState slowed =
                hud(600, false, false, false);
        assertEquals(MadeInHeavenPresentationPolicy.HudMode.SLOWED, slowed.mode());
        assertEquals(0.45D, slowed.viewerScale(), EPSILON);
        assertEquals(0.5D, slowed.progress(), EPSILON);
    }

    private static MadeInHeavenPresentationPolicy.HudState hud(
            int elapsed,
            boolean adapted,
            boolean theWorldActive,
            boolean theWorldUser
    ) {
        return MadeInHeavenPresentationPolicy.hudState(
                MadeInHeavenState.Phase.BUILDUP,
                elapsed,
                adapted,
                adapted ? 1.0D : MadeInHeavenCurves.physicalScale(elapsed),
                theWorldActive,
                theWorldUser
        );
    }
}
