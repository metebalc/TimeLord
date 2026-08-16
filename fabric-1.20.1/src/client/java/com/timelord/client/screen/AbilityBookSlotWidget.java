package com.timelord.client.screen;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.AbilityIconRegistry;
import com.timelord.client.TimeLordClient;
import com.timelord.client.state.ClientAbilityLoadoutState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class AbilityBookSlotWidget extends ClickableWidget {
    private final int slot;
    private final IntSupplier selectedSlot;
    private final IntConsumer onSelect;

    public AbilityBookSlotWidget(
            int x,
            int y,
            int width,
            int height,
            int slot,
            IntSupplier selectedSlot,
            IntConsumer onSelect
    ) {
        super(x, y, width, height, Text.empty());
        this.slot = slot;
        this.selectedSlot = selectedSlot;
        this.onSelect = onSelect;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onSelect.accept(slot);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean selected = selectedSlot.getAsInt() == slot;
        AbilityType ability = ClientAbilityLoadoutState.getEquipped(slot);
        int background = selected ? 0xFF704324 : isHovered() ? 0xFFD5AF70 : 0xFFC89555;
        int border = selected ? 0xFFFFDD76 : 0xFF5A3923;
        int textColor = selected ? 0xFFFFE8A3 : 0xFF372316;

        context.fill(getX(), getY(), getX() + width, getY() + height, background);
        context.drawBorder(getX(), getY(), width, height, border);

        Text narration = Text.translatable(
                "screen.time-lord.book.slot",
                TimeLordClient.getSkillKeyName(slot),
                Text.translatable(ability.translationKey())
        );
        setMessage(narration);

        Identifier icon = AbilityIconRegistry.get(ability);
        if (icon != null)
            context.drawTexture(icon, getX() + 4, getY() + 4, 20, 20, 0, 0, 32, 32, 32, 32);

        int textX = getX() + 29;
        int textWidth = Math.max(8, width - 33);
        Text key = Text.literal(TimeLordClient.getSkillKeyName(slot));
        Text name = Text.translatable(ability.translationKey());
        context.drawText(
                client.textRenderer,
                key,
                textX,
                getY() + 4,
                textColor,
                false
        );
        context.drawText(
                client.textRenderer,
                Text.literal(client.textRenderer.trimToWidth(name.getString(), textWidth)),
                textX,
                getY() + 15,
                textColor,
                false
        );
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
