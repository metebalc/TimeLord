package com.timelord.client.mixin;

import net.minecraft.entity.LimbAnimator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {
    @Accessor("prevSpeed")
    float timeLord$getPrevSpeed();

    @Accessor("prevSpeed")
    void timeLord$setPrevSpeed(float speed);

    @Accessor("speed")
    float timeLord$getSpeed();

    @Accessor("speed")
    void timeLord$setSpeed(float speed);

    @Accessor("pos")
    float timeLord$getPos();

    @Accessor("pos")
    void timeLord$setPos(float position);
}
