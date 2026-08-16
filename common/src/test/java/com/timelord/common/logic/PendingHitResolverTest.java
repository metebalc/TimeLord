package com.timelord.common.logic;

import com.timelord.common.model.PendingHit;
import com.timelord.common.model.TemporalPosition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PendingHitResolverTest {
    @Test
    void aggregatesDamageWithoutLosingTargetOrAttackerOrder() {
        UUID targetA = UUID.randomUUID();
        UUID targetB = UUID.randomUUID();
        UUID attackerA = UUID.randomUUID();
        UUID attackerB = UUID.randomUUID();

        PendingHit first = hit(targetA, attackerA, 2.0F);
        PendingHit second = hit(targetA, attackerB, 3.0F);
        PendingHit third = hit(targetB, attackerA, 4.0F);
        PendingHit last = hit(targetA, attackerA, 5.0F);

        PendingHitResolver.ResolutionPlan plan = PendingHitResolver.resolve(List.of(first, second, third, last));

        assertEquals(List.of(targetA, targetB), plan.targets().stream()
                .map(PendingHitResolver.TargetResolution::targetId)
                .toList());

        PendingHitResolver.TargetResolution targetAResolution = plan.targets().get(0);
        assertEquals(List.of(attackerA, attackerB), targetAResolution.damageContributions().stream()
                .map(PendingHitResolver.DamageContribution::attackerId)
                .toList());
        assertEquals(7.0F, targetAResolution.damageContributions().get(0).damage());
        assertEquals(3.0F, targetAResolution.damageContributions().get(1).damage());
        assertSame(last, targetAResolution.lastHit());
    }

    private static PendingHit hit(UUID targetId, UUID attackerId, float damage) {
        return new PendingHit(
                UUID.randomUUID(),
                targetId,
                attackerId,
                damage,
                TemporalPosition.ZERO,
                new TemporalPosition(1.0D, 0.0D, 0.0D)
        );
    }
}
