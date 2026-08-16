package com.timelord.client.hook;

/** Stores the weather renderer frame held while The World is active. */
public final class WeatherFreezeController {
    private boolean captured;
    private int ticks;
    private float tickDelta;

    public void capture(boolean active, int currentTicks, float currentTickDelta) {
        if (!active) {
            captured = false;
            return;
        }

        if (!captured) {
            captured = true;
            ticks = currentTicks;
            tickDelta = currentTickDelta;
        }
    }

    public int ticks(boolean active, int currentTicks) {
        return active && captured ? ticks : currentTicks;
    }

    public float tickDelta(boolean active, float currentTickDelta) {
        return active && captured ? tickDelta : currentTickDelta;
    }
}
