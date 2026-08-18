package com.timelord.client.hook;

import net.minecraft.entity.projectile.ProjectileEntity;

/** Leaves vanilla projectile prediction intact; temporal presentation is render-only. */
public final class ClientProjectileTickController {
    private ClientProjectileTickController() {}

    public static boolean shouldTick(ProjectileEntity projectile) {
        return true;
    }

    public static void endClientTick() {}

    public static void clear() {}
}
