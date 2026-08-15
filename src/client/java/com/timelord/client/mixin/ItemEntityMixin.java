package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Unique
    private boolean timeLord$rotationFrozen;

    @Unique
    private float timeLord$frozenRotation;

    @Inject(method = "getRotation", at = @At("RETURN"), cancellable = true)
    private void timeLord$freezeItemRotation(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (!TimeLordClient.isTheWorldActive()) {
            timeLord$rotationFrozen = false;
            return;
        }

        if (!timeLord$rotationFrozen) {
            timeLord$frozenRotation = cir.getReturnValue();
            timeLord$rotationFrozen = true;
        }

        cir.setReturnValue(
                timeLord$frozenRotation
        );
    }
}