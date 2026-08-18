package com.timelord.ability;

import com.timelord.mih.MadeInHeavenServerController;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Toggle boundary for the global Made in Heaven server controller. */
public final class MadeInHeavenAbility implements ToggleableAbility {
    @Override
    public boolean tryActivate(ServerPlayerEntity player) {
        if (isActive(player)) {
            if (!MadeInHeavenServerController.deactivate(player))
                return false;
            player.sendMessage(Text.literal("Made in Heaven: OFF"), true);
            return true;
        }

        if (!MadeInHeavenServerController.activate(player))
            return false;
        player.sendMessage(Text.literal("Made in Heaven: ON"), true);
        return true;
    }

    @Override
    public boolean isActive(ServerPlayerEntity player) {
        return MadeInHeavenServerController.isActiveUser(player.getUuid());
    }
}
