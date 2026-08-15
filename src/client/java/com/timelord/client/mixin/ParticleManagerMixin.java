package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Unique
    private float timeLord$frozenParticleTickDelta;

    @Unique
    private boolean timeLord$particleFrameCaptured;

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void timeLord$freezeParticles(
            CallbackInfo ci
    ) {
        if (TimeLordClient.isTheWorldActive()) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "renderParticles",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float timeLord$freezeParticleTickDelta(
            float tickDelta
    ) {
        if (!TimeLordClient.isTheWorldActive()) {
            timeLord$particleFrameCaptured = false;
            return tickDelta;
        }

        if (!timeLord$particleFrameCaptured) {
            timeLord$frozenParticleTickDelta = tickDelta;
            timeLord$particleFrameCaptured = true;
        }

        return timeLord$frozenParticleTickDelta;
    }
}