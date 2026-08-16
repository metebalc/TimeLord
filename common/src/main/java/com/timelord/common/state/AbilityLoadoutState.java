package com.timelord.common.state;

import com.timelord.common.ability.AbilityId;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Mutable loadout rules independent of player and networking APIs. */
public final class AbilityLoadoutState {
    private final AbilityId[] equipped;
    private final EnumSet<AbilityId> unlocked;

    public AbilityLoadoutState(AbilityId[] equipped, Set<AbilityId> unlocked) {
        Objects.requireNonNull(equipped, "equipped");
        Objects.requireNonNull(unlocked, "unlocked");
        if (equipped.length == 0)
            throw new IllegalArgumentException("A loadout must contain at least one slot");

        this.equipped = Arrays.copyOf(equipped, equipped.length);
        for (AbilityId ability : this.equipped)
            Objects.requireNonNull(ability, "equipped ability");

        this.unlocked = unlocked.isEmpty()
                ? EnumSet.noneOf(AbilityId.class)
                : EnumSet.copyOf(unlocked);
    }

    public int slotCount() {
        return equipped.length;
    }

    public AbilityId equippedAt(int slot) {
        checkSlot(slot);
        return equipped[slot];
    }

    public AbilityId[] equippedAbilities() {
        return Arrays.copyOf(equipped, equipped.length);
    }

    public Set<AbilityId> unlockedAbilities() {
        return Set.copyOf(unlocked);
    }

    public boolean isEquipped(AbilityId ability) {
        return findSlot(ability) >= 0;
    }

    public boolean isUnlocked(AbilityId ability) {
        return ability != null && unlocked.contains(ability);
    }

    /**
     * Equips an ability using the current swap behavior.
     *
     * @return whether the request was accepted and any ability that may need deactivation
     */
    public EquipResult equip(int slot, AbilityId ability) {
        checkSlot(slot);
        Objects.requireNonNull(ability, "ability");
        if (!unlocked.contains(ability))
            return EquipResult.rejected();

        AbilityId replaced = equipped[slot];
        int existingSlot = findSlot(ability);

        if (existingSlot >= 0 && existingSlot != slot)
            equipped[existingSlot] = replaced;

        equipped[slot] = ability;

        if (existingSlot < 0 && findSlot(replaced) < 0)
            return EquipResult.accepted(replaced);

        return EquipResult.accepted(null);
    }

    private int findSlot(AbilityId ability) {
        for (int slot = 0; slot < equipped.length; slot++) {
            if (equipped[slot] == ability)
                return slot;
        }

        return -1;
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= equipped.length)
            throw new IndexOutOfBoundsException("Invalid ability slot: " + slot);
    }

    public record EquipResult(boolean accepted, AbilityId abilityToDeactivate) {
        private static EquipResult rejected() {
            return new EquipResult(false, null);
        }

        private static EquipResult accepted(AbilityId abilityToDeactivate) {
            return new EquipResult(true, abilityToDeactivate);
        }
    }
}
