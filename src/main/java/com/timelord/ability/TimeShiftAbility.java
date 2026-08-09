package com.timelord.ability;

import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TimeShiftAbility implements Ability {

    private static final UUID TIME_SHIFT_SPEED_UUID = UUID.fromString("a8275680-5e1c-4e61-88db-93af224dd531");

    private static final Map<UUID, Mode> PLAYER_MODES = new HashMap<>();

    private enum Mode {
        OFF(0),
        X2(2),
        X3(3),
        X5(5),
        X10(10);

        private final int multiplier;

        Mode(int multiplier) {
            this.multiplier = multiplier;
        }

        public int multiplier() {
            return multiplier;
        }
    }

    @Override
    public void activate(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        Mode currentMode = PLAYER_MODES.getOrDefault(
                playerId,
                Mode.OFF
        );

        Mode nextMode = switch (currentMode) {
            case OFF -> Mode.X2;
            case X2 -> Mode.X3;
            case X3 -> Mode.X5;
            case X5 -> Mode.X10;
            case X10 -> Mode.OFF;
        };

        PLAYER_MODES.put(playerId, nextMode);

        applyMode(player, nextMode);
    }

    private static void applyMode(ServerPlayerEntity player, Mode mode) {
        EntityAttributeInstance speed =
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

        if (speed == null) {
            return;
        }

        // Remove previous Time Shift speed
        speed.removeModifier(TIME_SHIFT_SPEED_UUID);

        if (mode == Mode.OFF) {
            player.sendMessage(
                    Text.literal("Time Shift: OFF"),
                    true
            );

            playEffect(player);
            return;
        }

        double modifierAmount = mode.multiplier() - 1.0D;

        EntityAttributeModifier modifier =
                new EntityAttributeModifier(
                        TIME_SHIFT_SPEED_UUID,
                        "Time Shift Speed",
                        modifierAmount,
                        EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                );

        speed.addTemporaryModifier(modifier);

        player.sendMessage(
                Text.literal(
                        "Time Shift: " + mode.multiplier() + "x"
                ),
                true
        );

        playEffect(player);
    }

    private static void playEffect(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        world.spawnParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getBodyY(0.5D),
                player.getZ(),
                40,
                0.5D,
                0.9D,
                0.5D,
                0.08D
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.8F,
                1.6F
        );
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return PLAYER_MODES.getOrDefault(player.getUuid(), Mode.OFF) != Mode.OFF;
    }
}