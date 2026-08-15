package com.timelord.mixin;

import com.timelord.ability.TheWorldAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void timeLord$storeDamageDuringTimeStop(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!TheWorldAbility.isTimeStopped())
            return;

        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker))
            return;

        if (!TheWorldAbility.canMove(attacker))
            return;

        LivingEntity target = (LivingEntity) (Object) this;
        if (target instanceof ServerPlayerEntity targetPlayer && TheWorldAbility.canMove(targetPlayer))
            return;

        TheWorldAbility.storeHit(target, attacker, amount);

        cir.setReturnValue(false);
    }
}