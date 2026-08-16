package com.timelord.client.state;

import com.timelord.ability.AbilityManager.AbilityType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public final class ClientAbilityLoadoutState {
    public static final int SLOT_COUNT = 3;

    private static final AbilityType[] DEFAULT_LOADOUT = {
            AbilityType.SLOW_TIME,
            AbilityType.THE_WORLD,
            AbilityType.DIMENSION_CUT
    };

    private static final AbilityType[] EQUIPPED = Arrays.copyOf(DEFAULT_LOADOUT, SLOT_COUNT);
    private static final EnumSet<AbilityType> UNLOCKED = EnumSet.allOf(AbilityType.class);

    private ClientAbilityLoadoutState() {}

    public static void set(AbilityType[] equipped, Set<AbilityType> unlocked) {
        if (equipped.length == SLOT_COUNT)
            System.arraycopy(equipped, 0, EQUIPPED, 0, SLOT_COUNT);

        UNLOCKED.clear();
        UNLOCKED.addAll(unlocked);
    }

    public static AbilityType getEquipped(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT)
            throw new IndexOutOfBoundsException("Invalid ability slot: " + slot);

        return EQUIPPED[slot];
    }

    public static int findSlot(AbilityType ability) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (EQUIPPED[slot] == ability)
                return slot;
        }

        return -1;
    }

    public static boolean isUnlocked(AbilityType ability) {
        return UNLOCKED.contains(ability);
    }

    public static void reset() {
        System.arraycopy(DEFAULT_LOADOUT, 0, EQUIPPED, 0, SLOT_COUNT);
        UNLOCKED.clear();
        UNLOCKED.addAll(EnumSet.allOf(AbilityType.class));
    }
}
