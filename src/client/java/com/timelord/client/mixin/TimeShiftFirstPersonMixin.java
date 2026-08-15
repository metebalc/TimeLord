package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class TimeShiftFirstPersonMixin {
    //How fast the arm cycles.
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


    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void timeLord$applyTimeShiftRunMotion(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || player != client.player)
            return;

        int multiplier = TimeLordClient.getTimeShiftMultiplier();

        if (multiplier < 5)
            return;

        double velocityX = player.getVelocity().x;
        double velocityZ = player.getVelocity().z;
        double horizontalSpeedSquared = velocityX * velocityX + velocityZ * velocityZ;

        if (horizontalSpeedSquared < 0.035D)
            return;

        if (!timeLord$isRunningOnWater(player))
            return;

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
        float side = sin * horizontalAmount * handDirection;
        float vertical = -Math.abs(cos) * verticalAmount;
        float forward = cos * forwardAmount;

        matrices.translate(side, vertical, forward);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(sin * rollAmount * handDirection));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(cos * pitchAmount));
    }

    private boolean timeLord$isRunningOnWater(AbstractClientPlayerEntity player) {
        double[] offsets = {
                0.01D,
                0.10D,
                0.20D
        };

        for (double offset : offsets) {
            net.minecraft.util.math.BlockPos pos = net.minecraft.util.math.BlockPos.ofFloored(player.getX(),
                    player.getY() - offset, player.getZ());

            if (player.getWorld().getFluidState(pos).isIn(net.minecraft.registry.tag.FluidTags.WATER))
                return true;
        }
        return false;
    }

}