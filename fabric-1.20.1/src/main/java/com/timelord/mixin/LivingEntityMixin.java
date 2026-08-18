package com.timelord.mixin;

import com.timelord.hook.TheWorldDamageHook;
import com.timelord.mih.TemporalActionController;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void timeLord$storeDamageDuringTimeStop(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (TheWorldDamageHook.storeDelayedHit(target, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tickItemStackUsage", at = @At("HEAD"), cancellable = true)
    private void timeLord$enforceTemporalItemUseCadence(ItemStack stack, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player
                && !TemporalActionController.shouldAdvanceItemUse(player))
            ci.cancel();
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void timeLord$enforceTemporalJumpCadence(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player
                && !TemporalActionController.canJump(player))
            ci.cancel();
    }
}
