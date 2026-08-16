package com.timelord.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TheWorldHitRenderer {

    private static final Map<UUID, HitMark> HIT_MARKS =
            new LinkedHashMap<>();

    private static final long RESOLVE_DURATION_MS = 220L;
    private static final long RESOLVE_STAGGER_MS = 40L;

    private static final double BASE_SIZE = 0.22D;
    private static final double RESOLVE_SIZE = 0.75D;

    private TheWorldHitRenderer() {
    }

    public static void register() {
        WorldRenderEvents.LAST.register(
                TheWorldHitRenderer::render
        );
    }

    public static void addHit(
            UUID hitId,
            UUID targetId,
            UUID attackerId,
            Vec3d position,
            Vec3d attackDirection
    ) {
        HIT_MARKS.put(
                hitId,
                new HitMark(
                        hitId,
                        targetId,
                        attackerId,
                        position,
                        attackDirection,
                        false,
                        0L
                )
        );
    }

    public static void resolveHit(
            UUID hitId,
            int sequenceIndex
    ) {
        HitMark mark =
                HIT_MARKS.get(
                        hitId
                );

        if (mark == null
                || mark.resolving()) {
            return;
        }

        long resolveStartMs =
                System.currentTimeMillis()
                        + sequenceIndex
                        * RESOLVE_STAGGER_MS;

        HIT_MARKS.put(
                hitId,
                new HitMark(
                        mark.hitId(),
                        mark.targetId(),
                        mark.attackerId(),
                        mark.position(),
                        mark.attackDirection(),
                        true,
                        resolveStartMs
                )
        );
    }

    public static void clear() {
        HIT_MARKS.clear();
    }

    private static void render(
            WorldRenderContext context
    ) {
        if (HIT_MARKS.isEmpty())
            return;

        long now =
                System.currentTimeMillis();

        Iterator<Map.Entry<UUID, HitMark>> iterator =
                HIT_MARKS.entrySet().iterator();

        while (iterator.hasNext()) {
            HitMark mark =
                    iterator.next().getValue();

            if (!mark.resolving()
                    || now < mark.resolveStartMs()) {

                renderSuspendedMark(
                        context,
                        mark
                );

                continue;
            }

            float progress =
                    Math.min(
                            1.0F,
                            (now - mark.resolveStartMs())
                                    / (float) RESOLVE_DURATION_MS
                    );

            renderResolvingMark(
                    context,
                    mark,
                    progress
            );

            if (progress >= 1.0F) {
                iterator.remove();
            }
        }
    }

    private static void renderSuspendedMark(
            WorldRenderContext context,
            HitMark mark
    ) {
        renderStar(
                context,
                mark.position(),
                mark.attackDirection(),
                BASE_SIZE,
                0.85F,
                1.0F,
                0.90F,
                0.35F
        );

        renderStar(
                context,
                mark.position(),
                mark.attackDirection(),
                BASE_SIZE * 0.55D,
                1.0F,
                1.0F,
                1.0F,
                0.95F
        );
    }

    private static void renderResolvingMark(
            WorldRenderContext context,
            HitMark mark,
            float progress
    ) {
        float eased =
                1.0F
                        - (1.0F - progress)
                        * (1.0F - progress)
                        * (1.0F - progress);

        double size =
                BASE_SIZE
                        + (RESOLVE_SIZE - BASE_SIZE)
                        * eased;

        float alpha =
                1.0F - progress;

        renderStar(
                context,
                mark.position(),
                mark.attackDirection(),
                size,
                1.0F,
                0.92F,
                0.35F,
                alpha
        );

        renderStar(
                context,
                mark.position(),
                mark.attackDirection(),
                size * 0.55D,
                1.0F,
                1.0F,
                1.0F,
                Math.min(
                        1.0F,
                        alpha * 1.5F
                )
        );
    }

    private static void renderStar(
            WorldRenderContext context,
            Vec3d position,
            Vec3d attackDirection,
            double size,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        MatrixStack matrices =
                context.matrixStack();

        Vec3d camera =
                context.camera()
                        .getPos();

        matrices.push();

        matrices.translate(
                position.x - camera.x,
                position.y - camera.y,
                position.z - camera.z
        );

        Matrix4f matrix =
                matrices.peek()
                        .getPositionMatrix();

        Vec3d forward;

        if (attackDirection.lengthSquared()
                > 0.0001D) {

            forward =
                    attackDirection.normalize();

        } else {

            forward =
                    new Vec3d(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        }

        Vec3d reference;

        if (Math.abs(forward.y)
                > 0.95D) {

            reference =
                    new Vec3d(
                            1.0D,
                            0.0D,
                            0.0D
                    );

        } else {

            reference =
                    new Vec3d(
                            0.0D,
                            1.0D,
                            0.0D
                    );
        }

        Vec3d right =
                forward
                        .crossProduct(
                                reference
                        )
                        .normalize();

        Vec3d up =
                right
                        .crossProduct(
                                forward
                        )
                        .normalize();

        Vec3d diagonalA =
                right
                        .add(up)
                        .normalize();

        Vec3d diagonalB =
                right
                        .subtract(up)
                        .normalize();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(
                GameRenderer::getPositionColorProgram
        );

        BufferBuilder buffer =
                Tessellator
                        .getInstance()
                        .getBuffer();

        buffer.begin(
                VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR
        );

        addAxisLine(
                buffer,
                matrix,
                right,
                size,
                red,
                green,
                blue,
                alpha
        );

        addAxisLine(
                buffer,
                matrix,
                up,
                size,
                red,
                green,
                blue,
                alpha
        );

        addAxisLine(
                buffer,
                matrix,
                diagonalA,
                size * 0.72D,
                red,
                green,
                blue,
                alpha
        );

        addAxisLine(
                buffer,
                matrix,
                diagonalB,
                size * 0.72D,
                red,
                green,
                blue,
                alpha
        );

        BufferRenderer.drawWithGlobalProgram(
                buffer.end()
        );

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private static void addAxisLine(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3d axis,
            double size,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3d start =
                axis.multiply(
                        -size
                );

        Vec3d end =
                axis.multiply(
                        size
                );

        buffer.vertex(
                        matrix,
                        (float) start.x,
                        (float) start.y,
                        (float) start.z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .next();

        buffer.vertex(
                        matrix,
                        (float) end.x,
                        (float) end.y,
                        (float) end.z
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .next();
    }

    private record HitMark(
            UUID hitId,
            UUID targetId,
            UUID attackerId,
            Vec3d position,
            Vec3d attackDirection,
            boolean resolving,
            long resolveStartMs
    ) {
    }
}