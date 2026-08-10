package com.timelord.ability;

import com.timelord.ModSounds;
import com.timelord.time.TimeController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SlowTimeAbility implements Ability {
    private static final Map<UUID, Mode> SLOW_TIMES = new HashMap<>();
    private enum Mode {
        FIRST(0.5F, 8.0D),
        SECOND(0.25F, 12.0D),
        THIRD(0.1F, 16.0D);

        private final float timeScale;
        private final double radius;

        Mode(float timeScale, double radius) {
            this.timeScale = timeScale;
            this.radius = radius;
        }

        public float timeScale() {
            return timeScale;
        }

        public double radius() {
            return radius;
        }
    }

    public static void switchMode(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        Mode currentMode = SLOW_TIMES.getOrDefault(
                playerId,
                Mode.FIRST
        );

        Mode nextMode = switch (currentMode) {
            case FIRST -> Mode.SECOND;
            case SECOND -> Mode.THIRD;
            case THIRD -> Mode.FIRST;
        };

        SLOW_TIMES.put(playerId, nextMode);

        player.sendMessage(
                Text.literal("Slow Time Mode: " + nextMode),
                true
        );
    }

    private final int durationTicks;

    public SlowTimeAbility(int durationSeconds) {
        this.durationTicks = durationSeconds * 20;
    }

    @Override
    public void activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Mode mode = SLOW_TIMES.getOrDefault(player.getUuid(), Mode.FIRST);

        TimeController.slowTime(
                player,
                mode.timeScale(),
                durationTicks,
                mode.radius()
        );
        world.playSound(null, player.getBlockPos(), ModSounds.SLOW_TIME,
                SoundCategory.PLAYERS, 0.7F, 1.0F);
    }

    @Override
    public void deactivate(MinecraftServer server, ServerPlayerEntity player) {
        TimeController.resetTime(server, player.getUuid());
    }
}
