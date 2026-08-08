package com.timelord.mixin;

import com.timelord.time.TimeController;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void timeLord$slowIndependentEntity(Entity entity, CallbackInfo ci) {
        if (!TimeController.shouldTickEntity((ServerWorld) (Object) this, entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void timeLord$slowPassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (!TimeController.shouldTickEntity((ServerWorld) (Object) this, passenger)) {
            ci.cancel();
        }
    }
}
