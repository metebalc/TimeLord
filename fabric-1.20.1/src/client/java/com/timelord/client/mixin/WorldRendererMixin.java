package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import com.timelord.client.hook.WeatherFreezeController;
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
    private final WeatherFreezeController timeLord$weatherFreeze = new WeatherFreezeController();

    @Inject(method = "renderWeather", at = @At("HEAD"))
    private void timeLord$captureWeatherFrame(LightmapTextureManager manager, float tickDelta,
                                              double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        timeLord$weatherFreeze.capture(TimeLordClient.isTheWorldActive(), ticks, tickDelta);
    }

    @Redirect(method = "renderWeather", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;ticks:I"))
    private int timeLord$freezeWeatherTicks(WorldRenderer instance) {
        return timeLord$weatherFreeze.ticks(TimeLordClient.isTheWorldActive(), ticks);
    }

    @ModifyVariable(method = "renderWeather", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float timeLord$freezeWeatherTickDelta(float tickDelta) {
        return timeLord$weatherFreeze.tickDelta(TimeLordClient.isTheWorldActive(), tickDelta);
    }

    @Inject(method = "tickRainSplashing", at = @At("HEAD"), cancellable = true)
    private void timeLord$freezeRainSplashing(Camera camera, CallbackInfo ci) {
        if (TimeLordClient.isTheWorldActive())
            ci.cancel();
    }
}
