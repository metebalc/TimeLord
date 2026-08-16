package com.timelord.client.hook;

import com.timelord.client.TimeLordClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Applies the 1.20.1 first-person running motion used by Time Shift. */
public final class TimeShiftFirstPersonHook {
    private static final float X5_RUN_FREQUENCY = 0.65F;
    private static final float X10_RUN_FREQUENCY = 0.85F;
    private static final float X5_HORIZONTAL_AMOUNT = 0.035F;
    private static final float X10_HORIZONTAL_AMOUNT = 0.050F;
    private static final float X5_VERTICAL_AMOUNT = 0.030F;
    private static final float X10_VERTICAL_AMOUNT = 0.042F;
    private static final float X5_FORWARD_AMOUNT = 0.025F;
    private static final float X10_FORWARD_AMOUNT = 0.040F;
    private static final float X5_ROLL_AMOUNT = 2.5F;
    private static final float X10_ROLL_AMOUNT = 4.0F;
    private static final float X5_PITCH_AMOUNT = 1.8F;
    private static final float X10_PITCH_AMOUNT = 2.8F;

    private TimeShiftFirstPersonHook() {
    }

    public static void apply(
            AbstractClientPlayerEntity player,
            float tickDelta,
            Hand hand,
            MatrixStack matrices
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || player != client.player) {
            return;
        }

        int multiplier = TimeLordClient.getTimeShiftMultiplier();
        if (multiplier < 5) {
            return;
        }

        double velocityX = player.getVelocity().x;
        double velocityZ = player.getVelocity().z;
        if (velocityX * velocityX + velocityZ * velocityZ < 0.035D) {
            return;
        }

        if (!isRunningOnWater(player)) {
            return;
        }

        boolean x10 = multiplier >= 10;
        float frequency = x10 ? X10_RUN_FREQUENCY : X5_RUN_FREQUENCY;
        float horizontalAmount = x10 ? X10_HORIZONTAL_AMOUNT : X5_HORIZONTAL_AMOUNT;
        float verticalAmount = x10 ? X10_VERTICAL_AMOUNT : X5_VERTICAL_AMOUNT;
        float forwardAmount = x10 ? X10_FORWARD_AMOUNT : X5_FORWARD_AMOUNT;
        float rollAmount = x10 ? X10_ROLL_AMOUNT : X5_ROLL_AMOUNT;
        float pitchAmount = x10 ? X10_PITCH_AMOUNT : X5_PITCH_AMOUNT;
        float time = (player.age + tickDelta) * frequency;
        float sin = MathHelper.sin(time);
        float cos = MathHelper.cos(time);
        float handDirection = hand == Hand.MAIN_HAND ? 1.0F : -1.0F;

        matrices.translate(
                sin * horizontalAmount * handDirection,
                -Math.abs(cos) * verticalAmount,
                cos * forwardAmount
        );
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                sin * rollAmount * handDirection
        ));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cos * pitchAmount));
    }

    private static boolean isRunningOnWater(AbstractClientPlayerEntity player) {
        double[] offsets = {0.01D, 0.10D, 0.20D};
        for (double offset : offsets) {
            BlockPos pos = BlockPos.ofFloored(
                    player.getX(),
                    player.getY() - offset,
                    player.getZ()
            );
            if (player.getWorld().getFluidState(pos).isIn(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
