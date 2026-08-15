package com.timelord.client.mixin;

import com.timelord.client.TimeLordClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Unique
    private static final Map<UUID, Integer> timeLord$frozenAges = new HashMap<>();

    @Unique
    private static float timeLord$frozenTickDelta;

    @Unique
    private static boolean timeLord$tickDeltaFrozen;

    @Redirect(
            method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;getItemAge()I")
    )
    private int timeLord$freezeItemAge(ItemEntity item) {
        if (!TimeLordClient.isTheWorldActive()) {
            timeLord$frozenAges.remove(item.getUuid());
            return item.getItemAge();
        }
        return timeLord$frozenAges.computeIfAbsent(item.getUuid(), ignored -> item.getItemAge());
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private float timeLord$freezeItemTickDelta(float tickDelta) {
        if (!TimeLordClient.isTheWorldActive()) {
            timeLord$tickDeltaFrozen = false;
            return tickDelta;
        }

        if (!timeLord$tickDeltaFrozen) {
            timeLord$frozenTickDelta = tickDelta;
            timeLord$tickDeltaFrozen = true;
        }

        return timeLord$frozenTickDelta;
    }
}