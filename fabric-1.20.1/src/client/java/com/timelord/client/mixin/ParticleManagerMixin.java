package com.timelord.client.mixin;

import com.timelord.client.hook.FrozenFloatFrame;
import com.timelord.client.time.MadeInHeavenParticleClock;
import com.timelord.client.time.TheWorldClientState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Unique
    private final FrozenFloatFrame timeLord$particleFrame = new FrozenFloatFrame();

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void timeLord$freezeParticles(
            CallbackInfo ci
    ) {
        if (TheWorldClientState.isTimeStopped()) {
            ci.cancel();
            return;
        }
        MadeInHeavenParticleClock.beginClientTick();
    }

    @Redirect(
            method = "tickParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V")
    )
    private void timeLord$accelerateParticle(Particle particle) {
        int ticks = MadeInHeavenParticleClock.ticksThisFrame();
        for (int tick = 0; tick < ticks && particle.isAlive(); tick++)
            particle.tick();
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
        return timeLord$particleFrame.freeze(TheWorldClientState.isTimeStopped(), tickDelta);
    }
}
