package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private int ticks;

    @Unique
    private boolean timeLord$weatherFrozen;

    @Unique
    private int timeLord$frozenWeatherTicks;

    @Unique
    private float timeLord$frozenWeatherTickDelta;

    @Inject(method = "renderWeather", at = @At("HEAD"))
    private void timeLord$captureWeatherFrame(LightmapTextureManager manager, float tickDelta,
                                              double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!TimeLordClient.isTheWorldActive()) {
            timeLord$weatherFrozen = false;
            return;
        }

        if (!timeLord$weatherFrozen) {
            timeLord$weatherFrozen = true;
            timeLord$frozenWeatherTicks = ticks;
            timeLord$frozenWeatherTickDelta = tickDelta;
        }
    }

    @Redirect(method = "renderWeather", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;ticks:I"))
    private int timeLord$freezeWeatherTicks(WorldRenderer instance) {
        if (TimeLordClient.isTheWorldActive() && timeLord$weatherFrozen)
            return timeLord$frozenWeatherTicks;

        return ticks;
    }

    @ModifyVariable(method = "renderWeather", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float timeLord$freezeWeatherTickDelta(float tickDelta) {
        if (TimeLordClient.isTheWorldActive() && timeLord$weatherFrozen)
            return timeLord$frozenWeatherTickDelta;

        return tickDelta;
    }

    @Inject(method = "tickRainSplashing", at = @At("HEAD"), cancellable = true)
    private void timeLord$freezeRainSplashing(Camera camera, CallbackInfo ci) {
        if (TimeLordClient.isTheWorldActive())
            ci.cancel();
    }
}