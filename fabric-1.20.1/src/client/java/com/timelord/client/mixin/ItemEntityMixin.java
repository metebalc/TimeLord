package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import com.timelord.client.hook.FrozenFloatFrame;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Unique
    private final FrozenFloatFrame timeLord$rotationFrame = new FrozenFloatFrame();

    @Inject(method = "getRotation", at = @At("RETURN"), cancellable = true)
    private void timeLord$freezeItemRotation(float tickDelta, CallbackInfoReturnable<Float> cir) {
        boolean active = TimeLordClient.isTheWorldActive();
        float rotation = timeLord$rotationFrame.freeze(active, cir.getReturnValue());
        if (active) {
            cir.setReturnValue(rotation);
        }
    }
}
