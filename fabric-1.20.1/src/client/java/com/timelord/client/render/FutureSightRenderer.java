package com.timelord.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.timelord.client.state.ClientAbilityState;
import com.timelord.client.state.ClientFutureSightState;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.common.model.ThreatType;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

public final class FutureSightRenderer {
    private FutureSightRenderer() {}

    public static void register() {
        WorldRenderEvents.LAST.register(context -> {
            if (!ClientAbilityState.isActive(AbilityType.FUTURE_SIGHT))
                return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null)
                return;

            MatrixStack matrices = context.matrixStack();
            if (matrices == null)
                return;

            Vec3d camera = context.camera().getPos();
            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null)
                return;

            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            float pulse = 0.72F + 0.28F * (float) Math.sin(System.currentTimeMillis() / 90.0D);

            RenderSystem.disableDepthTest();

            for (Map.Entry<Integer, ThreatType> entry : ClientFutureSightState.getThreats().entrySet()) {
                Entity entity = client.world.getEntityById(entry.getKey());
                if (entity == null || entity.isRemoved())
                    continue;

                Box box = entity.getBoundingBox().expand(entry.getValue() == ThreatType.DANGEROUS_PROJECTILE ? 0.22D : 0.08D)
                        .offset(-camera.x, -camera.y, -camera.z);

                if (entry.getValue() == ThreatType.DANGEROUS_PROJECTILE) {
                    WorldRenderer.drawBox(matrices, lines, box, 1.0F, 0.12F, 0.05F, pulse);
                    Vec3d projected = entity.getVelocity().multiply(8.0D);
                    WorldRenderer.drawBox(
                            matrices,
                            lines,
                            box.offset(projected.x, projected.y, projected.z).expand(0.08D),
                            1.0F,
                            0.35F,
                            0.05F,
                            pulse * 0.75F
                    );
                } else {
                    WorldRenderer.drawBox(matrices, lines, box, 1.0F, 0.72F, 0.12F, 0.8F);
                }
            }

            if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                immediate.draw(RenderLayer.getLines());

            RenderSystem.enableDepthTest();
        });
    }
}
