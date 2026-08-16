package com.timelord.client.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

import java.util.UUID;

public final class TheWorldShockwaveRenderer {

    private static final long DURATION_MS = 900L;

    private static final double START_RADIUS = 0.35D;
    private static final double MAX_RADIUS = 24.0D;

    private static final int SEGMENTS = 128;

    private static UUID casterId;
    private static long startTimeMs;

    private static boolean active;

    private TheWorldShockwaveRenderer() {}

    public static void register() {
        WorldRenderEvents.LAST.register(TheWorldShockwaveRenderer::render);
    }

    public static void start(UUID caster) {
        casterId = caster;
        startTimeMs = System.currentTimeMillis();
        active = true;
    }

    public static void clear() {
        casterId = null;
        startTimeMs = 0L;
        active = false;
    }

    private static void render(WorldRenderContext context) {
        if (!active || casterId == null)
            return;

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null)
            return;

        Entity caster = client.world.getPlayerByUuid(casterId);

        if (caster == null)
            return;

        long elapsed = System.currentTimeMillis() - startTimeMs;

        float progress = Math.min(1.0F, elapsed / (float) DURATION_MS);

        if (progress >= 1.0F) {
            active = false;

            TheWorldRenderer.finishRemoteOpening();
            return;
        }

        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0F);
        double radius = START_RADIUS + (MAX_RADIUS - START_RADIUS) * eased;
        float alpha = 0.65F * (1.0F - progress);

        renderRing(context, caster.getPos(), radius, alpha);
        renderRing(context, caster.getPos(), radius * 0.94D, alpha * 0.45F);
        renderRing(context, caster.getPos(), radius * 1.06D, alpha * 0.30F);
    }

    private static void renderRing(WorldRenderContext context, Vec3d center, double radius, float alpha) {
        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();

        matrices.push();
        matrices.translate(center.x - camera.x, center.y + 1.0D - camera.y, center.z - camera.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / SEGMENTS;

            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);

            buffer.vertex(matrix, x, 0.0F, z).color(255, 230, 130, (int) (255.0F * alpha)).next();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }
}