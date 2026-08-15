package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import net.minecraft.client.particle.RainSplashParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RainSplashParticle.class)
public abstract class RainSplashParticleMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void timeLord$freezeRainSplash(CallbackInfo ci) {
        if (TimeLordClient.isTheWorldActive())
            ci.cancel();
    }
}