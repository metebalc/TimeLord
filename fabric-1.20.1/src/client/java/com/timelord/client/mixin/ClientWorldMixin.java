package com.timelord.client.mixin;

import com.timelord.client.hook.ClientEntityFreezeController;
import com.timelord.client.hook.ClientProjectileTickController;
import com.timelord.client.render.TemporalPlayerRenderController;
import com.timelord.client.render.TemporalProjectileRenderController;
import com.timelord.client.time.MadeInHeavenVisualWorldTime;
import com.timelord.client.time.TheWorldClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Redirect(
            method = {"getSkyBrightness", "getSkyColor", "getCloudsColor", "method_23787"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;getSkyAngle(F)F")
    )
    private float timeLord$visualSkyAngleForLighting(ClientWorld world, float tickDelta) {
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

    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void timeLord$freezeEntityHead(Entity entity, CallbackInfo ci) {
        if (entity instanceof ProjectileEntity projectile
                && !ClientProjectileTickController.shouldTick(projectile)) {
            ci.cancel();
            return;
        }
        ClientEntityFreezeController.beforeTick(entity);
    }

    @Inject(method = "tickEntity", at = @At("TAIL"))
    private void timeLord$freezeEntityTail(Entity entity, CallbackInfo ci) {
        ClientEntityFreezeController.afterTick(entity);
        if (entity instanceof PlayerEntity player)
            TemporalPlayerRenderController.record(player);
        if (entity instanceof ProjectileEntity projectile)
            TemporalProjectileRenderController.record(projectile);
    }
}
