package com.timelord.client.mixin;

import com.timelord.client.render.TemporalEchoVertexConsumers;
import com.timelord.client.render.TemporalPlayerRenderController;
import com.timelord.client.render.TemporalProjectileRenderController;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void timeLord$renderTemporalProjectile(
            Entity entity,
            double x,
            double y,
            double z,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!(entity instanceof ProjectileEntity projectile)
                || TemporalProjectileRenderController.isEchoPass())
            return;

        TemporalProjectileRenderController.prepare(projectile, tickDelta);
        EntityRenderDispatcher dispatcher = (EntityRenderDispatcher) (Object) this;
        for (TemporalProjectileRenderController.RenderEcho echo
                : TemporalProjectileRenderController.echoesFor(projectile, tickDelta)) {
            TemporalProjectileRenderController.beginEchoPass();
            try {
                dispatcher.render(
                        projectile,
                        x + echo.offset().x,
                        y + echo.offset().y,
                        z + echo.offset().z,
                        yaw,
                        tickDelta,
                        matrices,
                        new TemporalEchoVertexConsumers(vertexConsumers, echo.alpha()),
                        light
                );
            } finally {
                TemporalProjectileRenderController.endEchoPass();
            }
        }

        if (!TemporalProjectileRenderController.beginMainRender(
                projectile, tickDelta, matrices))
            ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void timeLord$endTemporalProjectileRender(
            Entity entity,
            double x,
            double y,
            double z,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (entity instanceof ProjectileEntity projectile
                && !TemporalProjectileRenderController.isEchoPass())
            TemporalProjectileRenderController.endMainRender(projectile, matrices);
    }

    @Inject(method = "renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/entity/Entity;FFLnet/minecraft/world/WorldView;F)V",
            at = @At("HEAD"), cancellable = true)
    private static void timeLord$suppressTemporalShadow(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            Entity entity,
            float opacity,
            float tickDelta,
            WorldView world,
            float radius,
            CallbackInfo ci
    ) {
        if (TemporalProjectileRenderController.isEchoPass()
                || TemporalPlayerRenderController.shouldSuppressShadow(entity))
            ci.cancel();
    }
}
