package com.timelord.client.mixin;

import com.timelord.client.render.TheWorldRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "onCameraEntitySet", at = @At("TAIL"))
    private void timeLord$restoreTheWorldShader(Entity entity, CallbackInfo ci) {
        TheWorldRenderer.refreshShader();
    }
}