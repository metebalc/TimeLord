package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteAtlasTexture.class)
public abstract class SpriteAtlasTextureMixin {
    @Inject(method = "tickAnimatedSprites", at = @At("HEAD"), cancellable = true)
    private void timeLord$freezeAnimatedTextures(CallbackInfo ci) {
        if (TimeLordClient.isTheWorldActive()) {
            ci.cancel();
        }
    }
}