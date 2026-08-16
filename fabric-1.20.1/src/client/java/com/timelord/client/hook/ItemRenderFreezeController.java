package com.timelord.client.hook;

import net.minecraft.entity.ItemEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Holds item age and interpolation inputs used by the 1.20.1 item renderer. */
public final class ItemRenderFreezeController {
    private final Map<UUID, Integer> frozenAges = new HashMap<>();
    private final FrozenFloatFrame tickDelta = new FrozenFloatFrame();

    public int itemAge(boolean active, ItemEntity item) {
        if (!active) {
            frozenAges.remove(item.getUuid());
            return item.getItemAge();
        }

        return frozenAges.computeIfAbsent(item.getUuid(), ignored -> item.getItemAge());
    }

    public float tickDelta(boolean active, float currentTickDelta) {
        return tickDelta.freeze(active, currentTickDelta);
    }
}
