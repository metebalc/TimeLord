package com.timelord.client.mixin;

import com.timelord.client.hook.ClientEntityFreezeController;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Inject(method = "tickEntity", at = @At("HEAD"))
    private void timeLord$freezeEntityHead(Entity entity, CallbackInfo ci) {
        ClientEntityFreezeController.beforeTick(entity);
    }

    @Inject(method = "tickEntity", at = @At("TAIL"))
    private void timeLord$freezeEntityTail(Entity entity, CallbackInfo ci) {
        ClientEntityFreezeController.afterTick(entity);
    }
}
