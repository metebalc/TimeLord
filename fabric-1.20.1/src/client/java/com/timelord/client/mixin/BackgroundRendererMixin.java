package com.timelord.client.mixin;

import com.timelord.client.time.MadeInHeavenVisualWorldTime;
import com.timelord.client.time.TheWorldClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps fog and sunrise/sunset coloring on the observer's visual world clock. */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Redirect(
            method = "render(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;getSkyAngle(F)F")
    )
    private static float timeLord$visualFogSkyAngle(ClientWorld world, float tickDelta) {
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

    @Redirect(
            method = "render(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;getSkyAngleRadians(F)F")
    )
    private static float timeLord$visualFogSkyAngleRadians(ClientWorld world, float tickDelta) {
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
}
