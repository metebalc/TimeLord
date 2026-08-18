package com.timelord.client.hud;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.AbilityIconRegistry;
import com.timelord.client.TimeLordClient;
import com.timelord.client.state.ClientAbilityState;
import com.timelord.client.state.ClientTimeRewindState;
import com.timelord.client.state.ClientAbilityStatus;
import com.timelord.client.time.ClientTimeField;
import com.timelord.client.time.MadeInHeavenClientState;
import com.timelord.client.time.TheWorldClientState;
import com.timelord.mih.MadeInHeavenPresentationPolicy;
import com.timelord.mih.MadeInHeavenState;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

import java.util.Locale;

public final class AbilityHudRenderer {
    private static final int ICON_SIZE = 26;
    private static final int SLOT_GAP = 4;
    private static final int SLOT_COUNT = 3;
    private static final int EDGE_MARGIN = 8;
    private static final int BASE_COLOR = 0xB8000000;
    private static final int BORDER_COLOR = 0xFF8A8A8A;
    private static final int ACTIVE_COLOR = 0xFFFFD85A;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private AbilityHudRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> render(context));
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden)
            return;

        int panelWidth = SLOT_COUNT * ICON_SIZE + (SLOT_COUNT - 1) * SLOT_GAP;
        int startX = EDGE_MARGIN;
        int y = Math.max(EDGE_MARGIN, client.getWindow().getScaledHeight() - ICON_SIZE - EDGE_MARGIN);

        renderMadeInHeavenPanel(context, client, startX, y - 28, Math.max(panelWidth, 164));
        renderTheWorldBar(context, client, startX, y - 9, panelWidth);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            AbilityType ability = TimeLordClient.getEquippedAbility(slot);
            int x = startX + slot * (ICON_SIZE + SLOT_GAP);
            renderSlot(context, client, ability, slot, x, y);
        }
    }

    private static void renderSlot(
            DrawContext context,
            MinecraftClient client,
            AbilityType ability,
            int slot,
            int x,
            int y
    ) {
        Identifier texture = AbilityIconRegistry.get(ability);
        boolean active = ClientAbilityStatus.isActive(ability);

        context.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, BASE_COLOR);
        if (texture != null)
            context.drawTexture(texture, x, y, ICON_SIZE, ICON_SIZE, 0, 0, 32, 32, 32, 32);

        RadialCooldownRenderer.draw(context, x, y, ICON_SIZE, ClientAbilityState.getCooldownProgress(ability));
        context.drawBorder(x - 1, y - 1, ICON_SIZE + 2, ICON_SIZE + 2, active ? ACTIVE_COLOR : BORDER_COLOR);

        String key = TimeLordClient.getSkillKeyName(slot);
        context.fill(x, y + ICON_SIZE - 10, x + 13, y + ICON_SIZE, 0xB8000000);
        context.drawTextWithShadow(client.textRenderer, key, x + 2, y + ICON_SIZE - 9, TEXT_COLOR);

        int cooldown = ClientAbilityState.getCooldown(ability);
        if (cooldown > 0) {
            String seconds = Integer.toString((cooldown + 19) / 20);
            int textX = x + (ICON_SIZE - client.textRenderer.getWidth(seconds)) / 2;
            context.drawTextWithShadow(client.textRenderer, seconds, textX, y + 12, TEXT_COLOR);
        }

        if (ability == AbilityType.TIME_SHIFT && TimeLordClient.getTimeShiftMultiplier() > 0) {
            String multiplier = TimeLordClient.getTimeShiftMultiplier() + "x";
            int textX = x + ICON_SIZE - client.textRenderer.getWidth(multiplier) - 2;
            context.fill(textX - 1, y, x + ICON_SIZE, y + 10, 0xB8000000);
            context.drawTextWithShadow(client.textRenderer, multiplier, textX, y + 1, 0xFF7DEBFF);
        }
    }

    private static void renderTheWorldBar(DrawContext context, MinecraftClient client, int x, int y, int width) {
        if (!TheWorldClientState.isTimeStopped() || TheWorldClientState.getMaxDurationTicks() <= 0)
            return;

        int remaining = TheWorldClientState.canMove(client.player.getUuid())
                ? TheWorldClientState.getRemainingTicks(client.player.getUuid())
                : TheWorldClientState.getLongestRemainingTicks();
        float progress = Math.min(1.0F, remaining / (float) TheWorldClientState.getMaxDurationTicks());
        int filled = Math.round(width * progress);

        context.fill(x, y, x + width, y + 5, 0xB8000000);
        context.fill(x, y, x + filled, y + 5, 0xFFE6CF65);
        context.drawBorder(x, y, width, 5, 0xFF8A8A8A);
    }

    private static void renderMadeInHeavenPanel(
            DrawContext context,
            MinecraftClient client,
            int x,
            int y,
            int minimumWidth
    ) {
        if (MadeInHeavenClientState.phase() == MadeInHeavenState.Phase.INACTIVE)
            return;

        boolean adapted = MadeInHeavenClientState.isActiveUser(client.player.getUuid());
        MadeInHeavenPresentationPolicy.HudState state =
                MadeInHeavenPresentationPolicy.hudState(
                        MadeInHeavenClientState.phase(),
                        MadeInHeavenClientState.elapsedActiveTicks(),
                        adapted,
                        MadeInHeavenClientState.perceptualScaleFor(client.player.getUuid()),
                        TheWorldClientState.isTimeStopped(),
                        TheWorldClientState.canMove(client.player.getUuid())
                );

        String text = hudText(state, MadeInHeavenClientState.elapsedActiveTicks());
        int width = Math.min(
                client.getWindow().getScaledWidth() - x - EDGE_MARGIN,
                Math.max(minimumWidth, client.textRenderer.getWidth(text) + 8)
        );
        if (width <= 0)
            return;

        int color = hudColor(state.mode());
        context.fill(x, y, x + width, y + 16, 0xC0000000);
        context.drawTextWithShadow(client.textRenderer, text, x + 4, y + 3, TEXT_COLOR);
        int filled = Math.max(0, Math.min(width, (int) Math.round(width * state.progress())));
        context.fill(x, y + 13, x + filled, y + 16, color);
        context.drawBorder(x, y, width, 16, 0xFF6E91A0);
    }

    private static String hudText(
            MadeInHeavenPresentationPolicy.HudState state,
            int elapsedTicks
    ) {
        String elapsed = String.format(Locale.ROOT, "%.1fs", elapsedTicks / 20.0D);
        String scale = String.format(Locale.ROOT, "%.2fx", state.viewerScale());
        String resistance = String.format(Locale.ROOT, "%.2fx", state.theWorldResistance());
        return switch (state.mode()) {
            case ADAPTED -> Text.translatable("hud.time-lord.mih.adapted", elapsed).getString();
            case SLOWED -> Text.translatable("hud.time-lord.mih.slowed", scale, elapsed).getString();
            case COLLAPSING -> Text.translatable("hud.time-lord.mih.collapsing", scale).getString();
            case THE_WORLD_DOMINANT -> Text.translatable("hud.time-lord.mih.world_dominant").getString();
            case RESISTING_THE_WORLD -> Text.translatable(
                    "hud.time-lord.mih.world_resistance", resistance).getString();
            case FROZEN_BY_THE_WORLD -> Text.translatable("hud.time-lord.mih.world_frozen").getString();
            case RESETTING -> Text.translatable("hud.time-lord.mih.resetting").getString();
        };
    }

    private static int hudColor(MadeInHeavenPresentationPolicy.HudMode mode) {
        return switch (mode) {
            case ADAPTED -> 0xFF78EEFF;
            case SLOWED -> 0xFF956BFF;
            case COLLAPSING -> 0xFF6FC6B1;
            case THE_WORLD_DOMINANT, FROZEN_BY_THE_WORLD -> 0xFFE6CF65;
            case RESISTING_THE_WORLD -> 0xFF70E7FF;
            case RESETTING -> 0xFFF8FCFF;
        };
    }
}
