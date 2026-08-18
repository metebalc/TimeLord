package com.timelord.client.hud;

import com.timelord.client.time.MadeInHeavenClientState;
import com.timelord.client.time.MadeInHeavenPresentationSettings;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Bounded screen-space reset presentation; it never delays authoritative gameplay. */
public final class MadeInHeavenCinematicRenderer {
    private MadeInHeavenCinematicRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register(MadeInHeavenCinematicRenderer::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        float alpha = MadeInHeavenPresentationSettings.cinematicAlpha(
                MadeInHeavenClientState.cinematicAlpha(
                        client.player.getUuid(), tickDelta));
        if (alpha <= 0.001F)
            return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int whiteAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        context.fill(0, 0, width, height, whiteAlpha << 24 | 0xF8FCFF);

        int edgeAlpha = Math.max(0, Math.min(150, Math.round(alpha * 150.0F)));
        int edgeHeight = Math.max(1, Math.round(alpha * 10.0F));
        int edgeColor = edgeAlpha << 24 | 0x8DEBFF;
        context.fill(0, 0, width, edgeHeight, edgeColor);
        context.fill(0, height - edgeHeight, width, height, edgeColor);
    }
}
