package com.timelord.client.hook;

/** Captures one render interpolation value for the duration of a freeze. */
public final class FrozenFloatFrame {
    private boolean captured;
    private float value;

    public float freeze(boolean active, float currentValue) {
        if (!active) {
            captured = false;
            return currentValue;
        }

        if (!captured) {
            value = currentValue;
            captured = true;
        }

        return value;
    }
}
