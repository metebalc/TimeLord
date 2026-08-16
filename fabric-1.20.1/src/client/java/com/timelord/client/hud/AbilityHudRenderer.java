package com.timelord.client.hud;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.AbilityIconRegistry;
import com.timelord.client.TimeLordClient;
import com.timelord.client.state.ClientAbilityState;
import com.timelord.client.state.ClientTimeRewindState;
import com.timelord.client.state.ClientAbilityStatus;
import com.timelord.client.time.ClientTimeField;
import com.timelord.client.time.TheWorldClientState;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

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
}
