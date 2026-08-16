package com.timelord.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.AbilityIconRegistry;
import com.timelord.client.TimeLordClient;
import com.timelord.client.ability.AbilityBookCatalog.Entry;
import com.timelord.client.state.ClientAbilityLoadoutState;
import com.timelord.client.state.ClientAbilityStatus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

public final class AbilityBookEntryWidget extends ClickableWidget {
    private static final int INK = 0xFF3A281C;
    private static final int MUTED_INK = 0xFF745D45;
    private static final int HOVER_COLOR = 0x80F6E3A8;
    private static final int SELECTED_GLOW = 0x70E6C75A;
    private static final int SELECTED_BORDER = 0xFFE5C85B;
    private static final int NORMAL_BORDER = 0xFF9B7A4E;

    private final Entry entry;
    private final Consumer<AbilityType> onEquip;

    public AbilityBookEntryWidget(int x, int y, int width, int height, Entry entry, Consumer<AbilityType> onEquip) {
        super(x, y, width, height, Text.translatable(entry.ability().translationKey()));
        this.entry = entry;
        this.onEquip = onEquip;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (ClientAbilityLoadoutState.isUnlocked(entry.ability()))
            onEquip.accept(entry.ability());
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        AbilityType ability = entry.ability();
        boolean unlocked = ClientAbilityLoadoutState.isUnlocked(ability);
        int equippedSlot = ClientAbilityLoadoutState.findSlot(ability);
        boolean selected = equippedSlot >= 0;

        active = unlocked;

        if (selected)
            context.fill(getX() - 2, getY() - 2, getX() + width + 2, getY() + height + 2, SELECTED_GLOW);

        context.fill(getX(), getY(), getX() + width, getY() + height, isHovered() ? HOVER_COLOR : 0x38E5CB8D);
        context.drawBorder(
                getX(),
                getY(),
                width,
                height,
                selected ? SELECTED_BORDER : NORMAL_BORDER
        );

        float alpha = unlocked ? 1.0F : 0.32F;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        Identifier icon = AbilityIconRegistry.get(ability);
        if (icon != null)
            context.drawTexture(icon, getX() + 5, getY() + 5, 32, 32, 0, 0, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int textColor = unlocked ? INK : MUTED_INK;
        int textX = getX() + 42;
        int textWidth = Math.max(20, width - 47);
        context.drawText(client.textRenderer, Text.translatable(ability.translationKey()), textX, getY() + 5, textColor, false);

        List<OrderedText> description = client.textRenderer.wrapLines(
                Text.translatable(entry.descriptionKey()),
                textWidth
        );
        int descriptionLines = height >= 65 ? 2 : 1;

        for (int line = 0; line < Math.min(descriptionLines, description.size()); line++)
            context.drawText(client.textRenderer, description.get(line), textX, getY() + 17 + line * 9, MUTED_INK, false);

        int metadataY = getY() + height - 19;
        Text keyText = equippedSlot >= 0
                ? Text.translatable("screen.time-lord.book.key", TimeLordClient.getSkillKeyName(equippedSlot))
                : Text.translatable("screen.time-lord.book.unbound");
        context.drawText(client.textRenderer, keyText, getX() + 5, metadataY, textColor, false);

        Text cooldown = ClientAbilityStatus.getCooldownText(ability);
        context.drawText(
                client.textRenderer,
                cooldown,
                getX() + width - client.textRenderer.getWidth(cooldown) - 5,
                metadataY,
                textColor,
                false
        );

        Text state = unlocked
                ? ClientAbilityStatus.getStateText(ability)
                : Text.translatable("screen.time-lord.book.locked");
        int stateColor = ClientAbilityStatus.isActive(ability) ? 0xFF8A5B10 : MUTED_INK;
        context.drawText(client.textRenderer, state, getX() + 5, getY() + height - 9, stateColor, false);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
