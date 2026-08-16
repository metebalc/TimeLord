package com.timelord.client.hud;

import net.minecraft.client.gui.DrawContext;

public final class RadialCooldownRenderer {
    private static final int OVERLAY_COLOR = 0xB8000000;

    private RadialCooldownRenderer() {}

    public static void draw(DrawContext context, int x, int y, int size, float progress) {
        if (progress <= 0.0F)
            return;

        float clamped = Math.min(1.0F, progress);
        double center = size / 2.0D;

        for (int row = 0; row < size; row++) {
            int runStart = -1;

            for (int column = 0; column <= size; column++) {
                boolean covered = column < size
                        && isCovered(column + 0.5D - center, row + 0.5D - center, clamped);

                if (covered && runStart < 0) {
                    runStart = column;
                } else if (!covered && runStart >= 0) {
                    context.fill(x + runStart, y + row, x + column, y + row + 1, OVERLAY_COLOR);
                    runStart = -1;
                }
            }
        }
    }

    private static boolean isCovered(double offsetX, double offsetY, float progress) {
        if (progress >= 1.0F)
            return true;

        double angle = Math.atan2(offsetX, -offsetY);
        if (angle < 0.0D)
            angle += Math.PI * 2.0D;

        return angle / (Math.PI * 2.0D) <= progress;
    }
}
