package com.timelord.client.time;

import com.timelord.mih.VisualTickAccumulator;
import com.timelord.mih.VisualWorldAccelerationPolicy;
import net.minecraft.client.MinecraftClient;

/** Client-only particle cadence derived from the synchronized observer state. */
public final class MadeInHeavenParticleClock {
    private static final VisualTickAccumulator ACCUMULATOR = new VisualTickAccumulator();
    private static int ticksThisFrame = 1;

    private MadeInHeavenParticleClock() {}

    public static void beginClientTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            clear();
            return;
        }
        double visualFactor = MadeInHeavenPresentationSettings.visualFactor(
                MadeInHeavenClientState.visualWorldFactorFor(client.player.getUuid()));
        double particleScale = VisualWorldAccelerationPolicy.particleTickScale(visualFactor);
        if (particleScale <= 1.0D)
            ACCUMULATOR.reset();
        ticksThisFrame = ACCUMULATOR.advance(particleScale);
    }

    public static int ticksThisFrame() {
        return ticksThisFrame;
    }

    public static void clear() {
        ACCUMULATOR.reset();
        ticksThisFrame = 1;
    }
}
