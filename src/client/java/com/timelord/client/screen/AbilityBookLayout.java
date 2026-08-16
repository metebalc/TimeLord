package com.timelord.client.screen;

public record AbilityBookLayout(
        int x,
        int y,
        int width,
        int height,
        int spineWidth,
        PageBounds leftPage,
        PageBounds rightPage
) {
    private static final int MAX_WIDTH = 470;
    private static final int MAX_HEIGHT = 270;
    private static final int OUTER_MARGIN = 10;
    private static final int SPINE_WIDTH = 10;

    public static AbilityBookLayout calculate(int screenWidth, int screenHeight) {
        int width = Math.min(MAX_WIDTH, Math.max(1, screenWidth - OUTER_MARGIN * 2));
        int height = Math.min(MAX_HEIGHT, Math.max(1, screenHeight - 28));
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        int pageWidth = (width - SPINE_WIDTH) / 2;

        PageBounds left = new PageBounds(x, y, pageWidth, height);
        PageBounds right = new PageBounds(x + pageWidth + SPINE_WIDTH, y, pageWidth, height);
        return new AbilityBookLayout(x, y, width, height, SPINE_WIDTH, left, right);
    }

    public record PageBounds(int x, int y, int width, int height) {}
}
