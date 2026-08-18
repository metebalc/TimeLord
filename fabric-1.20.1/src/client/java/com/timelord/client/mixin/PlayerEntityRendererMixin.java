package com.timelord.client.mixin;

import com.timelord.client.render.TemporalPlayerRenderController;
import com.timelord.client.render.TemporalEchoVertexConsumers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void timeLord$beginTemporalPresentation(
            AbstractClientPlayerEntity player,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (TemporalPlayerRenderController.isEchoPass())
            return;

        TemporalPlayerRenderController.prepareRender(player, tickDelta);
        timeLord$renderHistoricalEchoes(
                player, yaw, tickDelta, matrices, vertexConsumers, light);

        if (!TemporalPlayerRenderController.beginPreparedRender(player, tickDelta, matrices))
            ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void timeLord$endTemporalPresentation(
            AbstractClientPlayerEntity player,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (TemporalPlayerRenderController.isEchoPass())
            return;
        TemporalPlayerRenderController.endRender(player, matrices);
    }

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void timeLord$suppressHistoricalEchoLabel(
            AbstractClientPlayerEntity player,
            Text text,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (TemporalPlayerRenderController.isEchoPass())
            ci.cancel();
    }

    @Unique
    private void timeLord$renderHistoricalEchoes(
            AbstractClientPlayerEntity player,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        PlayerEntityRenderer renderer = (PlayerEntityRenderer) (Object) this;
        for (TemporalPlayerRenderController.RenderEcho echo
                : TemporalPlayerRenderController.echoesFor(player, tickDelta)) {
            matrices.push();
            matrices.translate(echo.offset().x, echo.offset().y, echo.offset().z);
            TemporalPlayerRenderController.beginEchoPass();
            try {
                renderer.render(
                        player,
                        yaw,
                        tickDelta,
                        matrices,
                        new TemporalEchoVertexConsumers(vertexConsumers, echo.alpha()),
                        light
                );
            } finally {
                TemporalPlayerRenderController.endEchoPass();
                matrices.pop();
            }
        }
    }
}
