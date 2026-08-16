package com.timelord.common.network.message;

import com.timelord.common.ability.AbilityId;

import java.util.List;

public final class AbilityMessages {
    private AbilityMessages() {}

    public record ActivateAbility(int abilityNetworkId) {}

    public record StartCharge(int abilityNetworkId) {}

    public record ReleaseCharge() {}

    public record SwitchSlowMode() {}

    public record CooldownUpdate(AbilityId ability, int remainingTicks) {}

    public record EquipAbility(int slot, AbilityId ability) {}

    public record LoadoutSync(List<AbilityId> equipped, List<AbilityId> unlocked) {
        public LoadoutSync {
            equipped = List.copyOf(equipped);
            unlocked = List.copyOf(unlocked);
        }
    }

    public record AbilityStateUpdate(
            AbilityId ability,
            boolean active,
            int remainingTicks,
            int totalTicks
    ) {}
}
