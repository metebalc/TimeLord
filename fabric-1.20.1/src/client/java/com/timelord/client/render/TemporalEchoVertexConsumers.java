package com.timelord.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.IdentityHashMap;
import java.util.Map;

/** Applies a cool temporal tint and bounded alpha to every part of an echo render pass. */
public final class TemporalEchoVertexConsumers implements VertexConsumerProvider {
    private static final float RED_TINT = 0.58F;
    private static final float GREEN_TINT = 0.86F;
    private static final float BLUE_TINT = 1.0F;

    private final VertexConsumerProvider delegate;
    private final float alpha;
    private final Map<RenderLayer, VertexConsumer> consumers = new IdentityHashMap<>();

    public TemporalEchoVertexConsumers(VertexConsumerProvider delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return consumers.computeIfAbsent(
                layer,
                ignored -> new FadingVertexConsumer(delegate.getBuffer(layer), alpha)
        );
    }

    private static final class FadingVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        private FadingVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(
                    tint(red, RED_TINT),
                    tint(green, GREEN_TINT),
                    tint(blue, BLUE_TINT),
                    tint(alpha, this.alpha)
            );
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void next() {
            delegate.next();
        }

        @Override
        public void fixedColor(int red, int green, int blue, int alpha) {
            delegate.fixedColor(
                    tint(red, RED_TINT),
                    tint(green, GREEN_TINT),
                    tint(blue, BLUE_TINT),
                    tint(alpha, this.alpha)
            );
        }

        @Override
        public void unfixColor() {
            delegate.unfixColor();
        }

        private static int tint(int component, float multiplier) {
            return Math.max(0, Math.min(255, Math.round(component * multiplier)));
        }
    }
}
