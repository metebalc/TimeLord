package com.timelord.mixin;

import com.timelord.hook.ServerWorldHooks;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void timeLord$slowIndependentEntity(Entity entity, CallbackInfo ci) {
        if (!ServerWorldHooks.shouldTickEntity((ServerWorld) (Object) this, entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void timeLord$slowPassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (!ServerWorldHooks.shouldTickEntity((ServerWorld) (Object) this, passenger)) {
            ci.cancel();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/tick/WorldTickScheduler;tick(JILjava/util/function/BiConsumer;)V"))
    private <T> void timeLord$freezeScheduledTicks(WorldTickScheduler<T> scheduler, long time, int maxTicks, BiConsumer<BlockPos, T> ticker) {
        ServerWorld world = (ServerWorld) (Object) this;

        if (ServerWorldHooks.shouldRunScheduledTicks(world, scheduler)) {
            scheduler.tick(time, maxTicks, ticker);
        }
    }
}
