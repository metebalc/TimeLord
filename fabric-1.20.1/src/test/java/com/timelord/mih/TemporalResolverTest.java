package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalResolverTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void madeInHeavenDoesNotChangeAuthoritativePlayerSpeed() {
        TemporalState adapted = resolve(false, false, true, true, 1200, 0.15D, 10.0D, 1.0D);
        TemporalState normal = resolve(false, false, true, false, 1200, 0.15D, 1.0D, 1.0D);

        assertEquals(10.0D, adapted.scale(), EPSILON);
        assertEquals(1.0D, normal.scale(), EPSILON);
    }

    @Test
    void timeShiftIsNotCappedByMadeInHeaven() {
        TemporalState x2 = resolve(false, false, true, false, 800, 0.30D, 2.0D, 1.0D);
        TemporalState x3 = resolve(false, false, true, false, 800, 0.30D, 3.0D, 1.0D);
        TemporalState x10 = resolve(false, false, true, false, 800, 0.30D, 10.0D, 1.0D);

        assertEquals(2.0D, x2.scale(), EPSILON);
        assertEquals(3.0D, x3.scale(), EPSILON);
        assertEquals(10.0D, x10.scale(), EPSILON);
    }

    @Test
    void slowTimeOpposesTheResolvedFrame() {
        TemporalState adaptedInField = resolve(false, false, true, true, 1000, 0.20D, 2.0D, 0.25D);
        assertEquals(0.50D, adaptedInField.scale(), EPSILON);
    }

    @Test
    void theWorldIsAnAbsolutePriorityWithLimitedLateResistance() {
        TemporalState worldUser = resolve(true, true, true, false, 1200, 0.15D, 10.0D, 0.1D);
        TemporalState normal = resolve(true, false, true, false, 1200, 0.15D, 10.0D, 1.0D);
        TemporalState earlyMih = resolve(true, false, true, true, 200, 0.90D, 10.0D, 1.0D);
        TemporalState lateMih = resolve(true, false, true, true, 1200, 0.15D, 10.0D, 1.0D);

        assertEquals(1.0D, worldUser.scale(), EPSILON);
        assertTrue(normal.isStopped());
        assertTrue(earlyMih.isStopped());
        assertEquals(0.08D, lateMih.scale(), EPSILON);
    }

    @Test
    void ordinaryTimeShiftRemainsPersonalAcceleration() {
        TemporalState ordinaryTime = resolve(false, false, false, false, 0, 1.0D, 5.0D, 1.0D);
        assertEquals(5.0D, ordinaryTime.scale(), EPSILON);
    }

    @Test
    void madeInHeavenResistanceInsideTheWorldIsNotAppliedTwice() {
        TemporalResolver.PlayerResolution resolution = resolvePolicy(
                true, false, true, true, 1200, 0.15D, 1.0D, 1.0D);

        assertEquals(0.08D, resolution.state().scale(), EPSILON);
        assertEquals(0.08D, resolution.wholeEntityTickScale(), EPSILON);
        assertEquals(1.0D, resolution.movementAttributeFactor(), EPSILON);
    }

    @Test
    void theWorldNeutralizesTimeShiftBeforeWholeTickEnforcement() {
        TemporalResolver.PlayerResolution resistantMih = resolvePolicy(
                true, false, true, true, 1200, 0.15D, 10.0D, 0.25D);
        TemporalResolver.PlayerResolution theWorldUser = resolvePolicy(
                true, true, true, true, 1200, 0.15D, 10.0D, 0.25D);
        TemporalResolver.PlayerResolution frozenNormal = resolvePolicy(
                true, false, true, false, 1200, 0.15D, 10.0D, 0.25D);

        assertEquals(0.1D, resistantMih.movementAttributeFactor(), EPSILON);
        assertEquals(0.08D, resistantMih.wholeEntityTickScale(), EPSILON);
        assertEquals(0.1D, theWorldUser.movementAttributeFactor(), EPSILON);
        assertEquals(1.0D, theWorldUser.wholeEntityTickScale(), EPSILON);
        assertEquals(0.0D, frozenNormal.wholeEntityTickScale(), EPSILON);
        assertTrue(frozenNormal.isWholeEntityTickStopped());
    }

    @Test
    void ordinaryMihDoesNotContributeToPhysicalComposition() {
        TemporalResolver.PlayerResolution resolution = resolvePolicy(
                false, false, true, false, 800, 0.30D, 2.0D, 0.5D);

        assertEquals(1.0D, resolution.state().scale(), EPSILON);
        assertEquals(0.5D, resolution.movementAttributeFactor(), EPSILON);
        assertEquals(1.0D, resolution.wholeEntityTickScale(), EPSILON);
    }

    @Test
    void collapsePerceptualScaleDoesNotChangePhysicalResolution() {
        TemporalState collapsing = resolve(false, false, true, false, 1000, 0.55D, 1.0D, 1.0D);
        assertEquals(1.0D, collapsing.scale(), EPSILON);
    }

    @Test
    void stoppedTimeNeverDividesByZero() {
        RelativeTemporalFactor entityStopped = RelativeTemporalFactor.between(
                TemporalState.running(1.0D), TemporalState.stopped());
        RelativeTemporalFactor viewerStopped = RelativeTemporalFactor.between(
                TemporalState.stopped(), TemporalState.running(1.0D));

        assertEquals(RelativeTemporalFactor.Relation.ENTITY_STOPPED, entityStopped.relation());
        assertEquals(RelativeTemporalFactor.Relation.VIEWER_STOPPED, viewerStopped.relation());
        assertEquals(0.0D, entityStopped.factor(), EPSILON);
        assertEquals(0.0D, viewerStopped.factor(), EPSILON);
    }

    @Test
    void stationaryEntitiesAreAlwaysReacquired() {
        RelativeTemporalFactor extreme = RelativeTemporalFactor.between(
                TemporalState.running(0.15D), TemporalState.running(1.0D));

        assertEquals(TemporalRenderMode.TEMPORAL_SKIP, TemporalRenderMode.select(extreme, true));
        assertEquals(TemporalRenderMode.NORMAL, TemporalRenderMode.select(extreme, false));
    }

    private static TemporalState resolve(
            boolean theWorldActive,
            boolean theWorldUser,
            boolean madeInHeavenActive,
            boolean madeInHeavenUser,
            int elapsedTicks,
            double normalScale,
            double timeShiftMultiplier,
            double slowTimeFactor
    ) {
        return TemporalResolver.resolvePlayer(new TemporalResolver.PlayerContext(
                theWorldActive,
                theWorldUser,
                madeInHeavenActive,
                madeInHeavenUser,
                elapsedTicks,
                timeShiftMultiplier,
                slowTimeFactor
        ));
    }

    private static TemporalResolver.PlayerResolution resolvePolicy(
            boolean theWorldActive,
            boolean theWorldUser,
            boolean madeInHeavenActive,
            boolean madeInHeavenUser,
            int elapsedTicks,
            double normalScale,
            double timeShiftMultiplier,
            double slowTimeFactor
    ) {
        return TemporalResolver.resolvePlayerPolicy(new TemporalResolver.PlayerContext(
                theWorldActive,
                theWorldUser,
                madeInHeavenActive,
                madeInHeavenUser,
                elapsedTicks,
                timeShiftMultiplier,
                slowTimeFactor
        ));
    }
}
