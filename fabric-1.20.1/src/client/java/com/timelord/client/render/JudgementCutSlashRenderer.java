package com.timelord.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import com.timelord.client.ClientJudgementCut;
import com.timelord.client.judgement.SlashInstance;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

public final class JudgementCutSlashRenderer {

    private static final Identifier SLASH_TEXTURE = Identifier.of("time-lord", "textures/judgement_slash.png");
    private static final Identifier DISTORTION_TEXTURE = Identifier.of("time-lord", "textures/judgement_distortion.png");

    private JudgementCutSlashRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
                if (!ClientJudgementCut.hasSuspendedCuts())
                    return;

                MatrixStack matrices = context.matrixStack();

                if (matrices == null)
                    return;

                MinecraftClient client = MinecraftClient.getInstance();
                Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

                long elapsed = System.currentTimeMillis() - ClientJudgementCut.getReleaseTime();
                float appearProgress = Math.min(1.0F, elapsed / 100.0F);
                appearProgress = 1.0F - (1.0F - appearProgress) * (1.0F - appearProgress);
                float pulse = 0.94F + (float) Math.sin(elapsed * 0.025D) * 0.06F;

                for (SlashInstance slash : ClientJudgementCut.getSlashes()) {
                    renderSlash(
                            matrices,
                            cameraPos,
                            slash,
                            appearProgress,
                            pulse
                    );
                }
            }
        );
    }

    private static void renderSlash(
            MatrixStack matrices,
            Vec3d cameraPos,
            SlashInstance slash,
            float appearProgress,
            float pulse
    ) {
        matrices.push();

        Vec3d position = slash.position();

        matrices.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(slash.yaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(slash.pitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(slash.roll()));

        float length = slash.length() * appearProgress * pulse;
        float width = slash.width() * appearProgress;

        renderLayer(
                matrices,
                DISTORTION_TEXTURE,
                length * 1.35F,
                width * 5.0F,
                100,
                180,
                255,
                45
        );

        renderLayer(
                matrices,
                SLASH_TEXTURE,
                length * 1.12F,
                width * 2.7F,
                50,
                150,
                255,
                100
        );

        int alpha = Math.min(255, (int) (230.0F * slash.intensity()));

        renderLayer(
                matrices,
                SLASH_TEXTURE,
                length,
                width,
                210,
                235,
                255,
                alpha
        );

        renderLayer(
                matrices,
                SLASH_TEXTURE,
                length * 0.95F,
                width * 0.35F,
                255,
                255,
                255,
                240
        );
        matrices.pop();
    }

    private static void renderLayer(
            MatrixStack matrices,
            Identifier texture,
            float length,
            float width,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE
        );

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, texture);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float halfLength = length * 0.5F;
        float halfWidth = width * 0.5F;

        buffer.vertex(matrix, -halfLength, -halfWidth, 0.0F)
                .texture(0.0F, 1.0F)
                .color(red, green, blue, alpha)
                .next();

        buffer.vertex(matrix, halfLength, -halfWidth, 0.0F)
                .texture(1.0F, 1.0F)
                .color(red, green, blue, alpha)
                .next();

        buffer.vertex(matrix, halfLength, halfWidth, 0.0F)
                .texture(1.0F, 0.0F)
                .color(red, green, blue, alpha)
                .next();

        buffer.vertex(matrix, -halfLength, halfWidth, 0.0F)
                .texture(0.0F, 0.0F)
                .color(red, green, blue, alpha)
                .next();

        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}