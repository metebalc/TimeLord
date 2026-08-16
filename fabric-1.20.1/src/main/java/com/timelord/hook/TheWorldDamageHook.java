package com.timelord.hook;

import com.timelord.ability.TheWorldAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Minecraft 1.20.1 damage interception for The World delayed-hit behavior.
 */
public final class TheWorldDamageHook {
    private TheWorldDamageHook() {
    }

    public static boolean storeDelayedHit(
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        if (!TheWorldAbility.isTimeStopped()) {
            return false;
        }

        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) {
            return false;
        }

        if (!TheWorldAbility.canMove(attacker)) {
            return false;
        }

        if (target instanceof ServerPlayerEntity targetPlayer
                && TheWorldAbility.canMove(targetPlayer)) {
            return false;
        }

        TheWorldAbility.storeHit(target, attacker, amount);
        return true;
    }
}
