package com.timelord.common.state;

import com.timelord.common.ability.AbilityId;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityStateTest {
    @Test
    void aTimedAbilityExpiresOnItsFinalTick() {
        AbilityState state = AbilityState.activeFor(2);

        state = state.tick();
        assertTrue(state.active());
        assertEquals(1, state.remainingTicks());

        state = state.tick();
        assertFalse(state.active());
        assertEquals(0, state.remainingTicks());
        assertEquals(2, state.elapsedTicks());
    }

    @Test
    void loadoutEquipPreservesTheExistingSwapRules() {
        AbilityLoadoutState loadout = new AbilityLoadoutState(
                new AbilityId[] { AbilityId.SLOW_TIME, AbilityId.THE_WORLD, AbilityId.DIMENSION_CUT },
                EnumSet.allOf(AbilityId.class)
        );

        AbilityLoadoutState.EquipResult swap = loadout.equip(0, AbilityId.THE_WORLD);
        assertTrue(swap.accepted());
        assertNull(swap.abilityToDeactivate());
        assertEquals(AbilityId.THE_WORLD, loadout.equippedAt(0));
        assertEquals(AbilityId.SLOW_TIME, loadout.equippedAt(1));

        AbilityLoadoutState.EquipResult replacement = loadout.equip(2, AbilityId.TIME_REWIND);
        assertTrue(replacement.accepted());
        assertEquals(AbilityId.DIMENSION_CUT, replacement.abilityToDeactivate());
    }
}
