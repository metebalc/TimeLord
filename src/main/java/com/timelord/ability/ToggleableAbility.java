package com.timelord.ability;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ToggleableAbility extends ConditionalAbility {
    boolean isActive(ServerPlayerEntity player);
}
