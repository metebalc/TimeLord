package com.timelord.ability;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.network.AbilityLoadoutNetworking;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AbilityLoadoutManager {
    public static final int SLOT_COUNT = 3;

    private static final AbilityType[] DEFAULT_LOADOUT = {
            AbilityType.SLOW_TIME,
            AbilityType.THE_WORLD,
            AbilityType.DIMENSION_CUT
    };

    private static final Map<UUID, AbilityType[]> LOADOUTS = new HashMap<>();
    private static final Map<UUID, EnumSet<AbilityType>> UNLOCKED_ABILITIES = new HashMap<>();

    private AbilityLoadoutManager() {}

    public static boolean equip(ServerPlayerEntity player, int slot, AbilityType ability) {
        if (slot < 0 || slot >= SLOT_COUNT || ability == null || !isUnlocked(player, ability)) {
            sync(player);
            return false;
        }

        AbilityType[] loadout = getMutableLoadout(player);
        AbilityType replaced = loadout[slot];
        int existingSlot = findSlot(loadout, ability);

        if (existingSlot >= 0 && existingSlot != slot)
            loadout[existingSlot] = replaced;

        loadout[slot] = ability;

        if (existingSlot < 0 && replaced != null && findSlot(loadout, replaced) < 0)
            AbilityManager.deactivateIfActive(player, replaced);

        sync(player);
        return true;
    }

    public static boolean isEquipped(ServerPlayerEntity player, AbilityType ability) {
        return findSlot(getMutableLoadout(player), ability) >= 0;
    }

    public static boolean isUnlocked(ServerPlayerEntity player, AbilityType ability) {
        return getUnlocked(player).contains(ability);
    }

    public static AbilityType[] getLoadout(ServerPlayerEntity player) {
        return Arrays.copyOf(getMutableLoadout(player), SLOT_COUNT);
    }

    public static Set<AbilityType> getUnlockedAbilities(ServerPlayerEntity player) {
        return EnumSet.copyOf(getUnlocked(player));
    }

    public static void sync(ServerPlayerEntity player) {
        AbilityLoadoutNetworking.send(player, getLoadout(player), getUnlockedAbilities(player));
    }

    private static AbilityType[] getMutableLoadout(ServerPlayerEntity player) {
        return LOADOUTS.computeIfAbsent(
                player.getUuid(),
                ignored -> Arrays.copyOf(DEFAULT_LOADOUT, SLOT_COUNT)
        );
    }

    private static EnumSet<AbilityType> getUnlocked(ServerPlayerEntity player) {
        return UNLOCKED_ABILITIES.computeIfAbsent(
                player.getUuid(),
                ignored -> EnumSet.allOf(AbilityType.class)
        );
    }

    private static int findSlot(AbilityType[] loadout, AbilityType ability) {
        for (int slot = 0; slot < loadout.length; slot++) {
            if (loadout[slot] == ability)
                return slot;
        }

        return -1;
    }
}
