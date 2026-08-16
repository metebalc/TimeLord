package com.timelord.future;

import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.ability.TheWorldAbility;
import com.timelord.common.logic.ThreatMath;
import com.timelord.common.model.ThreatInfo;
import com.timelord.common.model.ThreatType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public final class ThreatDetector {
    private static final double RANGE = 24.0D;

    private ThreatDetector() {}

    public static List<ThreatInfo> findThreats(ServerPlayerEntity player) {
        if (TheWorldAbility.isTimeStopped())
            return List.of();

        Box searchArea = player.getBoundingBox().expand(RANGE);
        List<Entity> nearby = player.getServerWorld().getOtherEntities(
                player,
                searchArea,
                entity -> entity instanceof ProjectileEntity || entity instanceof HostileEntity
        );
        List<ThreatInfo> threats = new ArrayList<>();

        for (Entity entity : nearby) {
            if (entity instanceof ProjectileEntity projectile && isDangerousProjectile(player, projectile)) {
                threats.add(new ThreatInfo(projectile.getId(), ThreatType.DANGEROUS_PROJECTILE));
            } else if (entity instanceof HostileEntity hostile && isRelevantHostile(player, hostile)) {
                threats.add(new ThreatInfo(hostile.getId(), ThreatType.HOSTILE_MOB));
            }
        }

        return threats;
    }

    private static boolean isDangerousProjectile(ServerPlayerEntity player, ProjectileEntity projectile) {
        if (projectile.getOwner() == player)
            return false;

        return ThreatMath.isDangerousProjectile(
                TemporalPositionAdapter.fromMinecraft(player.getBoundingBox().getCenter()),
                TemporalPositionAdapter.fromMinecraft(projectile.getPos()),
                TemporalPositionAdapter.fromMinecraft(projectile.getVelocity())
        );
    }

    private static boolean isRelevantHostile(ServerPlayerEntity player, HostileEntity hostile) {
        if (!hostile.isAlive())
            return false;

        return ThreatMath.isRelevantHostile(
                TemporalPositionAdapter.fromMinecraft(player.getPos()),
                TemporalPositionAdapter.fromMinecraft(hostile.getPos()),
                TemporalPositionAdapter.fromMinecraft(hostile.getVelocity()),
                hostile.getTarget() == player
        );
    }
}
