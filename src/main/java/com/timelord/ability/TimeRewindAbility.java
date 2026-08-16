package com.timelord.ability;

import com.timelord.network.TimeRewindNetworking;
import com.timelord.rewind.PlayerStateHistory;
import com.timelord.rewind.PlayerStateSnapshot;
import com.timelord.rewind.SafeRewindPositionFinder;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class TimeRewindAbility implements ConditionalAbility {
    private static final int REWIND_TICKS = 60;
    private static final int EFFECT_TICKS = 10;

    @Override
    public boolean tryActivate(ServerPlayerEntity player) {
        if (TheWorldAbility.isTimeStopped() && !TheWorldAbility.canMove(player)) {
            player.sendMessage(Text.literal("Cannot rewind while frozen in stopped time"), true);
            return false;
        }

        if (player.hasVehicle() || player.isSleeping()) {
            player.sendMessage(Text.literal("Cannot rewind in the current state"), true);
            return false;
        }

        Optional<PlayerStateSnapshot> stored = PlayerStateHistory.get(player, REWIND_TICKS);
        if (stored.isEmpty()) {
            player.sendMessage(Text.literal("Not enough history to rewind"), true);
            return false;
        }

        PlayerStateSnapshot snapshot = stored.get();
        if (!snapshot.world().equals(player.getWorld().getRegistryKey())) {
            player.sendMessage(Text.literal("Cannot rewind across dimensions"), true);
            return false;
        }

        ServerWorld world = player.getServerWorld();
        Optional<Vec3d> safePosition = SafeRewindPositionFinder.find(world, player, snapshot.position());
        if (safePosition.isEmpty()) {
            player.sendMessage(Text.literal("No safe rewind position found"), true);
            return false;
        }

        AbilityManager.cancelCharging(player);
        TimeShiftAbility.cancelTransientState(player);

        Vec3d origin = player.getPos();
        Vec3d destination = safePosition.get();

        player.networkHandler.requestTeleport(
                destination.x,
                destination.y,
                destination.z,
                snapshot.yaw(),
                snapshot.pitch()
        );
        player.setVelocity(snapshot.velocity());
        player.velocityModified = true;
        player.setHealth(Math.min(snapshot.health(), player.getMaxHealth()));
        player.fallDistance = 0.0F;

        TimeRewindNetworking.sendEffect(player, origin, destination, EFFECT_TICKS);
        return true;
    }
}
