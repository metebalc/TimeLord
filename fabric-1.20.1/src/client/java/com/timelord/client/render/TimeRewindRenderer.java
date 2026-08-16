package com.timelord.client.render;

import com.timelord.client.state.ClientTimeRewindState;
import com.timelord.common.model.TemporalPosition;
import com.timelord.common.state.TimeRewindEffectState;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;

public final class TimeRewindRenderer {
    private TimeRewindRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TimeRewindRenderer::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) {
            ClientTimeRewindState.clear();
            return;
        }

        for (TimeRewindEffectState effect : ClientTimeRewindState.getEffects()) {
            float progress = effect.progress();

            for (int index = 0; index < 4; index++) {
                double offset = (index + progress) / 4.0D;
                double clampedOffset = Math.min(1.0D, offset);
                TemporalPosition point = effect.origin().add(
                        effect.destination().subtract(effect.origin()).multiply(clampedOffset));
                client.world.addParticle(
                        ParticleTypes.REVERSE_PORTAL,
                        point.x(),
                        point.y() + 0.9D,
                        point.z(),
                        0.0D,
                        0.02D,
                        0.0D
                );
            }
        }

        ClientTimeRewindState.tick();
    }
}
