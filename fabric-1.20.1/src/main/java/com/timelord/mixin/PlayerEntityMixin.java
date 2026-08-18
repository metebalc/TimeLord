package com.timelord.mixin;

import com.timelord.mih.TemporalActionController;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void timeLord$enforceMadeInHeavenAttackCadence(Entity target, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player
                && !TemporalActionController.canAttack(player))
            ci.cancel();
    }
}
