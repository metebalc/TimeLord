package com.timelord.client.mixin;

import com.timelord.client.hook.WeatherFreezeController;
import com.timelord.client.time.MadeInHeavenClientState;
import com.timelord.client.time.MadeInHeavenVisualWorldTime;
import com.timelord.client.time.TheWorldClientState;
import com.timelord.mih.VisualWorldAccelerationPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
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
    private final WeatherFreezeController timeLord$worldFrame = new WeatherFreezeController();

    @Inject(method = "renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V", at = @At("HEAD"))
    private void timeLord$captureWeatherFrame(LightmapTextureManager manager, float tickDelta,
                                              double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        timeLord$captureWorldFrame(tickDelta);
    }

    @Redirect(method = "renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;ticks:I"))
    private int timeLord$freezeWeatherTicks(WorldRenderer instance) {
        return timeLord$worldFrame.ticks(
                TheWorldClientState.isTimeStopped(), timeLord$visualTicks());
    }

    @ModifyVariable(method = "renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float timeLord$freezeWeatherTickDelta(float tickDelta) {
        return timeLord$worldFrame.tickDelta(
                TheWorldClientState.isTimeStopped(), timeLord$visualTickDelta(tickDelta));
    }

    @Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"))
    private void timeLord$captureCloudFrame(net.minecraft.client.util.math.MatrixStack matrices,
                                            org.joml.Matrix4f projectionMatrix, float tickDelta,
                                            double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        timeLord$captureWorldFrame(tickDelta);
    }

    @Redirect(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;ticks:I"))
    private int timeLord$visualCloudTicks(WorldRenderer instance) {
        return timeLord$worldFrame.ticks(
                TheWorldClientState.isTimeStopped(), timeLord$visualTicks());
    }

    @ModifyVariable(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float timeLord$visualCloudTickDelta(float tickDelta) {
        return timeLord$worldFrame.tickDelta(
                TheWorldClientState.isTimeStopped(), timeLord$visualTickDelta(tickDelta));
    }

    @Inject(method = "tickRainSplashing", at = @At("HEAD"), cancellable = true)
    private void timeLord$freezeRainSplashing(Camera camera, CallbackInfo ci) {
        if (TheWorldClientState.isTimeStopped())
            ci.cancel();
    }

    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getSkyAngle(F)F"))
    private float timeLord$madeInHeavenSkyAngle(ClientWorld world, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return world.getSkyAngle(tickDelta);
        return MadeInHeavenVisualWorldTime.skyAngle(
                world,
                client.player.getUuid(),
                tickDelta,
                world.getSkyAngle(tickDelta),
                TheWorldClientState.isTimeStopped()
        );
    }

    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getSkyAngleRadians(F)F"))
    private float timeLord$madeInHeavenSkyAngleRadians(ClientWorld world, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return world.getSkyAngleRadians(tickDelta);
        return MadeInHeavenVisualWorldTime.skyAngleRadians(
                world,
                client.player.getUuid(),
                tickDelta,
                world.getSkyAngleRadians(tickDelta),
                TheWorldClientState.isTimeStopped()
        );
    }

    @Unique
    private void timeLord$captureWorldFrame(float tickDelta) {
        timeLord$worldFrame.capture(
                TheWorldClientState.isTimeStopped(),
                timeLord$visualTicks(),
                timeLord$visualTickDelta(tickDelta)
        );
    }

    @Unique
    private int timeLord$visualTicks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return ticks;
        return VisualWorldAccelerationPolicy.visualRendererTicks(
                ticks,
                MadeInHeavenClientState.visualWorldOffsetTicksFor(client.player.getUuid())
        );
    }

    @Unique
    private float timeLord$visualTickDelta(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return tickDelta;
        return VisualWorldAccelerationPolicy.visualTickDelta(
                tickDelta,
                MadeInHeavenClientState.visualWorldFactorFor(client.player.getUuid())
        );
    }
}
