package com.timelord.adapter;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.common.ability.AbilityId;

/** Bridges the current 1.20.1 ability enum to the common protocol enum. */
public final class AbilityIdAdapter {
    private AbilityIdAdapter() {}

    public static AbilityId toCommon(AbilityType ability) {
        return ability == null ? null : AbilityId.fromNetworkId(ability.networkId());
    }

    public static AbilityType toMinecraft(AbilityId ability) {
        return ability == null ? null : AbilityType.fromNetworkId(ability.networkId());
    }
}
