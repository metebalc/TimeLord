package com.timelord.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityStepHeightAccessor {
    @Accessor("stepHeight")
    void timeLord$setStepHeight(float height);

    @Accessor("stepHeight")
    float timeLord$getStepHeight();
}