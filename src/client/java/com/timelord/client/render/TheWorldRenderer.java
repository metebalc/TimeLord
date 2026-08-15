package com.timelord.client.render;

import com.timelord.client.mixin.GameRendererAccessor;
import com.timelord.client.mixin.GameRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public final class TheWorldRenderer {

    private static final Identifier OPENING_SHADER =
            new Identifier("time-lord", "shaders/post/the_world_opening.json");

    private static final Identifier FROZEN_SHADER =
            new Identifier("minecraft", "shaders/post/desaturate.json");

    private static final long OPENING_DURATION_MS = 900L;

    private static boolean worldActive;
    private static boolean openingActive;
    private static boolean frozenShaderLoaded;

    private static long openingStartMs;

    private TheWorldRenderer() {}

    public static void setActive(boolean active) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (active && !worldActive) {
            worldActive = true;
            openingActive = true;
            frozenShaderLoaded = false;
            openingStartMs = System.currentTimeMillis();

            loadOpeningShader(client);
            return;
        }

        if (!active) {
            worldActive = false;
            openingActive = false;
            frozenShaderLoaded = false;
            openingStartMs = 0L;

            client.gameRenderer.disablePostProcessor();
        }
    }

    public static void tick() {
        if (!worldActive)
            return;

        if (!openingActive)
            return;

        long elapsed = System.currentTimeMillis() - openingStartMs;

        if (elapsed < OPENING_DURATION_MS)
            return;

        openingActive = false;

        loadFrozenShader(MinecraftClient.getInstance());
    }

    private static void loadOpeningShader(MinecraftClient client) {
        client.gameRenderer.disablePostProcessor();

        ((GameRendererAccessor) client.gameRenderer).timeLord$loadPostProcessor(OPENING_SHADER);
    }

    private static void loadFrozenShader(MinecraftClient client) {
        loadFrozenShader(client, false);
    }

    private static void loadFrozenShader(MinecraftClient client, boolean force) {
        if (frozenShaderLoaded && !force)
            return;

        frozenShaderLoaded = true;

        client.gameRenderer.disablePostProcessor();
        ((GameRendererAccessor) client.gameRenderer)
                .timeLord$loadPostProcessor(
                        FROZEN_SHADER
                );
    }

    public static boolean isOpening() {
        return openingActive;
    }

    public static boolean isActive() {
        return worldActive;
    }

    public static float getOpeningProgress() {
        if (!openingActive)
            return 1.0F;

        long elapsed = System.currentTimeMillis() - openingStartMs;

        return Math.min(1.0F, elapsed / (float) OPENING_DURATION_MS);
    }

    public static void setRemoteActive(boolean active) {
        if (!active) {
            setActive(false);
            return;
        }

        worldActive = true;
        openingActive = false;
        frozenShaderLoaded = false;
        openingStartMs = 0L;
    }

    public static void finishRemoteOpening() {
        if (!worldActive)
            return;

        loadFrozenShader(MinecraftClient.getInstance());
    }

    public static void refreshShader() {
        if (!worldActive)
            return;

        MinecraftClient client = MinecraftClient.getInstance();

        if (openingActive) {
            loadOpeningShader(client);
            return;
        }

        loadFrozenShader(client, true);
    }

}