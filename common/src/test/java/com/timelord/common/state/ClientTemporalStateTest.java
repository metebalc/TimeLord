package com.timelord.common.state;

import com.timelord.common.model.TemporalPosition;
import com.timelord.common.model.ThreatInfo;
import com.timelord.common.model.ThreatType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTemporalStateTest {
    @Test
    void rewindEffectUsesTheExistingRenderThenDecrementLifecycle() {
        TimeRewindEffectState effect = TimeRewindEffectState.start(
                UUID.randomUUID(),
                TemporalPosition.ZERO,
                new TemporalPosition(10.0D, 0.0D, 0.0D),
                2
        );

        assertEquals(0.0F, effect.progress());
        effect = effect.tick();
        assertFalse(effect.expired());
        assertEquals(0.5F, effect.progress());
        effect = effect.tick();
        assertTrue(effect.expired());
    }

    @Test
    void futureSightStatePreservesWireOrderAndReplacesDuplicateEntityIds() {
        FutureSightThreatState state = new FutureSightThreatState();

        state.replace(List.of(
                new ThreatInfo(7, ThreatType.HOSTILE_MOB),
                new ThreatInfo(3, ThreatType.DANGEROUS_PROJECTILE),
                new ThreatInfo(7, ThreatType.DANGEROUS_PROJECTILE)
        ));

        assertEquals(List.of(7, 3), state.threats().keySet().stream().toList());
        assertEquals(ThreatType.DANGEROUS_PROJECTILE, state.threats().get(7));
    }
}
