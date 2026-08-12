package com.timelord.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.timelord.client.ClientJudgementCut;
import com.timelord.client.time.ClientTimeField;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SlowTimeFieldRenderer {
    private static final Identifier SPHERE_TEXTURE = Identifier.of("time-lord", "textures/time_sphere.png");
    private static final Identifier RING_TEXTURE = Identifier.of("time-lord", "textures/time_ring.png");
    private static final long EXPAND_DURATION_MS = 4000L;
    private static final Map<UUID, Long> FIELD_START_TIMES = new HashMap<>();

    private SlowTimeFieldRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {

            boolean timeFieldActive = ClientTimeField.isActive();
            boolean judgementCutActive = ClientJudgementCut.exists();

            if (!timeFieldActive && !judgementCutActive) {
                FIELD_START_TIMES.clear();
                return;
            }

            MatrixStack matrices = context.matrixStack();

            if (matrices == null) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

            long currentTime = System.currentTimeMillis();

            /*
             * SLOW TIME
             */
            if (timeFieldActive) {

                for (Map.Entry<UUID, ClientTimeField.Field> entry
                        : ClientTimeField.getFields().entrySet()) {

                    UUID fieldId = entry.getKey();
                    ClientTimeField.Field field = entry.getValue();

                    long startTime =
                            FIELD_START_TIMES.computeIfAbsent(
                                    fieldId,
                                    id -> currentTime
                            );

                    float progress =
                            Math.min(
                                    1.0F,
                                    (currentTime - startTime)
                                            / (float) EXPAND_DURATION_MS
                            );

                    float eased =
                            progress * progress
                                    * (3.0F - 2.0F * progress);

                    double animatedRadius =
                            field.radius() * eased;

                    renderSphere(
                            matrices,
                            cameraPos,
                            field.center(),
                            animatedRadius
                    );

                    renderRing(
                            matrices,
                            cameraPos,
                            field.center(),
                            animatedRadius * 1.25D
                    );
                }

                FIELD_START_TIMES.keySet().removeIf(
                        id -> !ClientTimeField
                                .getFields()
                                .containsKey(id)
                );
            }

            /*
             * THE JUDGEMENT CUT
             */
            if (judgementCutActive) {

                double radius;

                if (ClientJudgementCut.isCharging()) {

                    long elapsed =
                            currentTime
                                    - ClientJudgementCut.getStartTime();

                    double progress =
                            Math.min(
                                    1.0D,
                                    elapsed / 3000.0D
                            );

                    progress =
                            progress * progress
                                    * (3.0D - 2.0D * progress);

                    radius =
                            2.5D
                                    + (10.0D - 2.5D)
                                    * progress;

                } else {

                    radius =
                            ClientJudgementCut.getFinalRadius();
                }

                Vec3d center =
                        ClientJudgementCut.getCenter();

                renderSphere(
                        matrices,
                        cameraPos,
                        center,
                        radius
                );

                renderRing(
                        matrices,
                        cameraPos,
                        center,
                        radius * 1.15D
                );
            }
        });
    }

    private static void renderSphere(
            MatrixStack matrices,
            Vec3d cameraPos,
            Vec3d center,
            double radius
    ) {
        matrices.push();
        matrices.translate(
                center.x - cameraPos.x,
                center.y - cameraPos.y,
                center.z - cameraPos.z
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, SPHERE_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int latitudeSegments = 24;
        int longitudeSegments = 48;

        float time = (System.currentTimeMillis() % 100000L) / 1000.0F;
        float textureOffset = time * 0.02F;
        for (int lat = 0; lat < latitudeSegments; lat++) {
            double theta1 = Math.PI * lat / latitudeSegments - Math.PI / 2.0;
            double theta2 = Math.PI * (lat + 1) / latitudeSegments - Math.PI / 2.0;

            float v1 = (float) lat / latitudeSegments;
            float v2 = (float) (lat + 1) / latitudeSegments;

            for (int lon = 0; lon < longitudeSegments; lon++) {
                double phi1 = Math.PI * 2.0D * lon / longitudeSegments;
                double phi2 = Math.PI * 2.0D * (lon + 1) / longitudeSegments;

                float u1 = (float) lon / longitudeSegments + textureOffset;
                float u2 = (float) (lon + 1) / longitudeSegments + textureOffset;

                vertex(
                        buffer,
                        matrix,
                        theta1,
                        phi1,
                        radius,
                        u1,
                        v1
                );

                vertex(
                        buffer,
                        matrix,
                        theta1,
                        phi2,
                        radius,
                        u2,
                        v1
                );

                vertex(
                        buffer,
                        matrix,
                        theta2,
                        phi2,
                        radius,
                        u2,
                        v2
                );

                vertex(
                        buffer,
                        matrix,
                        theta2,
                        phi1,
                        radius,
                        u1,
                        v2
                );
            }
        }
        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private static void vertex(
            BufferBuilder buffer,
            Matrix4f matrix,
            double theta,
            double phi,
            double radius,
            float u,
            float v
    ) {
        float x = (float) (Math.cos(theta) * Math.cos(phi) * radius);
        float y = (float) (Math.sin(theta) * radius);
        float z = (float) (Math.cos(theta) * Math.sin(phi) * radius);

        buffer.vertex(matrix, x, y, z)
                .texture(u, v)
                .color(50, 130, 255, 80)
                .next();
    }

    private static void renderRing(
            MatrixStack matrices,
            Vec3d cameraPos,
            Vec3d center,
            double radius
    ) {
        matrices.push();

        matrices.translate(
                center.x - cameraPos.x,
                center.y - cameraPos.y,
                center.z - cameraPos.z
        );

        float time = (System.currentTimeMillis() % 100000L) / 1000.0F;
        float rotation = time * 35.0F;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, RING_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = (float) radius;

        buffer.vertex(matrix, -r, 0.0F, -r)
                .texture(0.0F, 0.0F)
                .color(80, 170, 255, 180)
                .next();

        buffer.vertex(matrix, r, 0.0F, -r)
                .texture(1.0F, 0.0F)
                .color(80, 170, 255, 180)
                .next();

        buffer.vertex(matrix, r, 0.0F, r)
                .texture(1.0F, 1.0F)
                .color(80, 170, 255, 180)
                .next();

        buffer.vertex(matrix, -r, 0.0F, r)
                .texture(0.0F, 1.0F)
                .color(80, 170, 255, 180)
                .next();

        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }
}
