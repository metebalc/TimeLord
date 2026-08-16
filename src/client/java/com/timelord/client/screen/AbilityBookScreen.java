package com.timelord.client.screen;

import com.timelord.client.TimeLordClient;
import com.timelord.client.ability.AbilityBookCatalog;
import com.timelord.client.ability.AbilityBookCatalog.Entry;
import com.timelord.client.ability.AbilityCategory;
import com.timelord.client.screen.AbilityBookLayout.PageBounds;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AbilityBookScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 3;
    private static final int SLOT_COUNT = 3;
    private static final int PAGE_INK = 0xFF3A281C;

    private final List<CategoryPage> pages = buildPages();
    private int spread;
    private int targetSlot;
    private AbilityBookLayout layout;

    public AbilityBookScreen() {
        this(0, 0);
    }

    private AbilityBookScreen(int spread, int targetSlot) {
        super(Text.translatable("screen.time-lord.book.title"));
        this.spread = spread;
        this.targetSlot = targetSlot;
    }

    @Override
    protected void init() {
        layout = AbilityBookLayout.calculate(width, height);
        spread = Math.max(0, Math.min(spread, spreadCount() - 1));

        int slotGap = 4;
        int slotWidth = Math.max(58, (layout.width() - 28 - slotGap * 2) / SLOT_COUNT);
        int totalSlotWidth = slotWidth * SLOT_COUNT + slotGap * 2;
        int slotX = layout.x() + (layout.width() - totalSlotWidth) / 2;
        int slotY = layout.y() + 8;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            addDrawableChild(new AbilityBookSlotWidget(
                    slotX + slot * (slotWidth + slotGap),
                    slotY,
                    slotWidth,
                    28,
                    slot,
                    () -> targetSlot,
                    this::selectTargetSlot
            ));
        }

        addPageEntries(layout.leftPage(), pageAt(spread * 2));
        addPageEntries(layout.rightPage(), pageAt(spread * 2 + 1));

        int navigationY = layout.y() + layout.height() - 23;
        ButtonWidget previous = addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> changeSpread(-1))
                .dimensions(layout.leftPage().x() + 8, navigationY, 24, 18)
                .build());
        ButtonWidget next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> changeSpread(1))
                .dimensions(layout.rightPage().x() + layout.rightPage().width() - 32, navigationY, 24, 18)
                .build());
        previous.active = spread > 0;
        next.active = spread + 1 < spreadCount();

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(layout.x() + layout.width() / 2 - 30, navigationY, 60, 18)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        renderBook(context);
        renderPageHeading(context, layout.leftPage(), pageAt(spread * 2));
        renderPageHeading(context, layout.rightPage(), pageAt(spread * 2 + 1));
        super.render(context, mouseX, mouseY, delta);

        Text indicator = Text.translatable("screen.time-lord.book.spread", spread + 1, spreadCount());
        context.drawCenteredTextWithShadow(
                textRenderer,
                indicator,
                layout.x() + layout.width() / 2,
                layout.y() + layout.height() - 35,
                0xFFF5DDA7
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_3) {
            selectTargetSlot(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            changeSpread(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            changeSpread(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void renderBook(DrawContext context) {
        context.fill(layout.x() - 4, layout.y() - 4, layout.x() + layout.width() + 4, layout.y() + layout.height() + 4, 0xFF4A2B1A);
        renderPage(context, layout.leftPage(), true);
        renderPage(context, layout.rightPage(), false);

        int spineX = layout.leftPage().x() + layout.leftPage().width();
        context.fill(spineX, layout.y(), spineX + layout.spineWidth(), layout.y() + layout.height(), 0xFF2D1B15);
        context.fill(spineX, layout.y() + 3, spineX + 2, layout.y() + layout.height() - 3, 0x665C3926);
        context.fill(spineX + layout.spineWidth() - 2, layout.y() + 3, spineX + layout.spineWidth(), layout.y() + layout.height() - 3, 0x665C3926);
    }

    private void renderPage(DrawContext context, PageBounds bounds, boolean left) {
        context.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xFFF0D9A4);
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF7B5735);

        int shadeX = left ? bounds.x() + bounds.width() - 8 : bounds.x();
        context.fill(shadeX, bounds.y() + 2, shadeX + 8, bounds.y() + bounds.height() - 2, 0x30714A2E);
        for (int y = bounds.y() + 36; y < bounds.y() + bounds.height() - 25; y += 12)
            context.fill(bounds.x() + 7, y, bounds.x() + bounds.width() - 7, y + 1, 0x105E4028);
    }

    private void renderPageHeading(DrawContext context, PageBounds bounds, CategoryPage page) {
        if (page == null)
            return;
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable(page.category().translationKey()),
                bounds.x() + bounds.width() / 2,
                bounds.y() + 39,
                PAGE_INK
        );
    }

    private void addPageEntries(PageBounds bounds, CategoryPage page) {
        if (page == null)
            return;

        int x = bounds.x() + 8;
        int startY = bounds.y() + 51;
        int availableHeight = bounds.height() - 80;
        int gap = 4;
        int entryHeight = Math.min(64, Math.max(43, (availableHeight - gap * (page.entries().size() - 1)) / page.entries().size()));

        for (int index = 0; index < page.entries().size(); index++) {
            Entry entry = page.entries().get(index);
            addDrawableChild(new AbilityBookEntryWidget(
                    x,
                    startY + index * (entryHeight + gap),
                    bounds.width() - 16,
                    entryHeight,
                    entry,
                    ability -> TimeLordClient.equipAbility(targetSlot, ability)
            ));
        }
    }

    private void selectTargetSlot(int slot) {
        targetSlot = slot;
        clearAndInit();
    }

    private void changeSpread(int direction) {
        int next = spread + direction;
        if (next >= 0 && next < spreadCount()) {
            spread = next;
            clearAndInit();
        }
    }

    private CategoryPage pageAt(int index) {
        return index >= 0 && index < pages.size() ? pages.get(index) : null;
    }

    private int spreadCount() {
        return Math.max(1, (pages.size() + 1) / 2);
    }

    private static List<CategoryPage> buildPages() {
        List<CategoryPage> result = new ArrayList<>();
        for (AbilityCategory category : AbilityCategory.values()) {
            List<Entry> entries = AbilityBookCatalog.getByCategory(category);
            for (int from = 0; from < entries.size(); from += ENTRIES_PER_PAGE)
                result.add(new CategoryPage(category, entries.subList(from, Math.min(from + ENTRIES_PER_PAGE, entries.size()))));
        }
        return List.copyOf(result);
    }

    private record CategoryPage(AbilityCategory category, List<Entry> entries) {}
}
