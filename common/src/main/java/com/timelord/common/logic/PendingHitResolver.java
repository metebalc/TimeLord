package com.timelord.common.logic;

import com.timelord.common.model.PendingHit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Builds the ordered damage plan used when The World releases stored hits. */
public final class PendingHitResolver {
    private PendingHitResolver() {}

    public static ResolutionPlan resolve(List<PendingHit> hits) {
        Objects.requireNonNull(hits, "hits");
        Map<UUID, TargetAccumulator> targets = new LinkedHashMap<>();

        for (PendingHit hit : hits) {
            Objects.requireNonNull(hit, "hit");
            TargetAccumulator target = targets.computeIfAbsent(hit.targetId(), ignored -> new TargetAccumulator());
            target.damageByAttacker.merge(hit.attackerId(), hit.damage(), Float::sum);
            target.lastHit = hit;
        }

        List<TargetResolution> resolutions = new ArrayList<>(targets.size());
        for (Map.Entry<UUID, TargetAccumulator> targetEntry : targets.entrySet()) {
            List<DamageContribution> contributions = targetEntry.getValue().damageByAttacker.entrySet().stream()
                    .map(entry -> new DamageContribution(entry.getKey(), entry.getValue()))
                    .toList();

            resolutions.add(new TargetResolution(
                    targetEntry.getKey(),
                    contributions,
                    targetEntry.getValue().lastHit
            ));
        }

        return new ResolutionPlan(resolutions);
    }

    public record ResolutionPlan(List<TargetResolution> targets) {
        public ResolutionPlan {
            targets = List.copyOf(targets);
        }
    }

    public record TargetResolution(
            UUID targetId,
            List<DamageContribution> damageContributions,
            PendingHit lastHit
    ) {
        public TargetResolution {
            Objects.requireNonNull(targetId, "targetId");
            damageContributions = List.copyOf(damageContributions);
            Objects.requireNonNull(lastHit, "lastHit");
        }
    }

    public record DamageContribution(UUID attackerId, float damage) {
        public DamageContribution {
            Objects.requireNonNull(attackerId, "attackerId");
        }
    }

    private static final class TargetAccumulator {
        private final LinkedHashMap<UUID, Float> damageByAttacker = new LinkedHashMap<>();
        private PendingHit lastHit;
    }
}
