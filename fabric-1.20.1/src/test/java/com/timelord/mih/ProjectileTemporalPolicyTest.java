package com.timelord.mih;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectileTemporalPolicyTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void madeInHeavenProjectileKeepsTheNormalAuthoritativeFrame() {
        assertEquals(1.0D,
                ProjectileTemporalPolicy.scale(1.0D), EPSILON);
    }

    @Test
    void normalProjectileRemainsInTheVanillaAuthoritativeFrame() {
        assertEquals(1.0D,
                ProjectileTemporalPolicy.scale(1.0D), EPSILON);
    }

    @Test
    void localSlowTimeComposesWithoutExceedingOne() {
        assertEquals(0.5D,
                ProjectileTemporalPolicy.scale(0.5D), EPSILON);
        assertEquals(1.0D,
                ProjectileTemporalPolicy.scale(1.0D), EPSILON);
        assertEquals(0.4D,
                ProjectileTemporalPolicy.scale(0.4D), EPSILON);
    }

    @Test
    void slowRenderingInterpolatesOnlyBetweenKnownHistoricalSamples() {
        assertEquals(0.0D,
                ProjectileTemporalPolicy.historicalInterpolationProgress(20, 20, 14, 0.0D), EPSILON);
        assertEquals(0.5D,
                ProjectileTemporalPolicy.historicalInterpolationProgress(23, 20, 14, 0.0D), EPSILON);
        assertEquals(1.0D,
                ProjectileTemporalPolicy.historicalInterpolationProgress(30, 20, 14, 0.0D), EPSILON);
    }
}
