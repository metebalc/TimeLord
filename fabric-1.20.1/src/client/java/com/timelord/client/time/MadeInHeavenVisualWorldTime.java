package com.timelord.client.time;

import net.minecraft.client.world.ClientWorld;

import java.util.UUID;

/** Observer-specific celestial clock. It never mutates ClientWorld or server time. */
public final class MadeInHeavenVisualWorldTime {
    private static final float FULL_ROTATION_RADIANS = (float) (Math.PI * 2.0D);
    private static boolean frozen;
    private static double frozenVisualTime;

    private MadeInHeavenVisualWorldTime() {}

    public static float skyAngle(
            ClientWorld world,
            UUID viewerId,
            float tickDelta,
            float vanillaAngle,
            boolean theWorldActive
    ) {
        double offsetTicks = MadeInHeavenClientState.visualWorldOffsetTicksFor(viewerId);
        if (world.getDimension().hasFixedTime())
            return vanillaAngle;

        double visualFactor = MadeInHeavenClientState.visualWorldFactorFor(viewerId);
        double currentVisualTime = world.getTimeOfDay() + offsetTicks + tickDelta * visualFactor;
        if (theWorldActive) {
            if (!frozen) {
                frozen = true;
                frozenVisualTime = currentVisualTime;
            }
            currentVisualTime = frozenVisualTime;
        } else {
            frozen = false;
        }

        if (offsetTicks <= 0.0D && !theWorldActive)
            return vanillaAngle;
        long visualTime = (long) Math.floor(currentVisualTime);
        return world.getDimension().getSkyAngle(visualTime);
    }

    public static float skyAngleRadians(
            ClientWorld world,
            UUID viewerId,
            float tickDelta,
            float vanillaAngleRadians,
            boolean theWorldActive
    ) {
        float vanillaAngle = vanillaAngleRadians / FULL_ROTATION_RADIANS;
        return skyAngle(world, viewerId, tickDelta, vanillaAngle, theWorldActive)
                * FULL_ROTATION_RADIANS;
    }

    public static void clear() {
        frozen = false;
        frozenVisualTime = 0.0D;
    }
}
