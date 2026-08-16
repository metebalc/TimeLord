package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import com.timelord.client.hook.ItemRenderFreezeController;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Unique
    private static final ItemRenderFreezeController timeLord$freezeController =
            new ItemRenderFreezeController();

    @Redirect(
            method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;getItemAge()I")
    )
    private int timeLord$freezeItemAge(ItemEntity item) {
        return timeLord$freezeController.itemAge(TimeLordClient.isTheWorldActive(), item);
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private float timeLord$freezeItemTickDelta(float tickDelta) {
        return timeLord$freezeController.tickDelta(TimeLordClient.isTheWorldActive(), tickDelta);
    }
}
