package com.timelord.common.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AbilityIdTest {
    @Test
    void madeInHeavenUsesTheNextStableProtocolId() {
        AbilityId ability = AbilityId.MADE_IN_HEAVEN;

        assertEquals(6, ability.networkId());
        assertEquals(ability, AbilityId.fromNetworkId(6));
        assertEquals("ability.time-lord.made_in_heaven", ability.translationKey());
        assertEquals(0, ability.cooldownTicks());
        assertFalse(ability.isChargeable());
    }
}
