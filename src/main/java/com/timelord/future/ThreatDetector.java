package com.timelord.future;

import com.timelord.ability.TheWorldAbility;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class ThreatDetector {
    private static final double RANGE = 24.0D;
    private static final double PROJECTILE_LOOKAHEAD_TICKS = 40.0D;
    private static final double PROJECTILE_DANGER_RADIUS_SQUARED = 2.25D;

    private ThreatDetector() {}

    public static List<Threat> findThreats(ServerPlayerEntity player) {
        if (TheWorldAbility.isTimeStopped())
            return List.of();

        Box searchArea = player.getBoundingBox().expand(RANGE);
        List<Entity> nearby = player.getServerWorld().getOtherEntities(
                player,
                searchArea,
                entity -> entity instanceof ProjectileEntity || entity instanceof HostileEntity
        );
        List<Threat> threats = new ArrayList<>();

        for (Entity entity : nearby) {
            if (entity instanceof ProjectileEntity projectile && isDangerousProjectile(player, projectile)) {
                threats.add(new Threat(projectile.getId(), ThreatType.DANGEROUS_PROJECTILE));
            } else if (entity instanceof HostileEntity hostile && isRelevantHostile(player, hostile)) {
                threats.add(new Threat(hostile.getId(), ThreatType.HOSTILE_MOB));
            }
        }

        return threats;
    }

    private static boolean isDangerousProjectile(ServerPlayerEntity player, ProjectileEntity projectile) {
        if (projectile.getOwner() == player)
            return false;

        Vec3d velocity = projectile.getVelocity();
        double speedSquared = velocity.lengthSquared();
        if (speedSquared < 0.0025D)
            return false;

        Vec3d toPlayer = player.getBoundingBox().getCenter().subtract(projectile.getPos());
        double closestTime = toPlayer.dotProduct(velocity) / speedSquared;
        if (closestTime <= 0.0D || closestTime > PROJECTILE_LOOKAHEAD_TICKS)
            return false;

        Vec3d closestPoint = projectile.getPos().add(velocity.multiply(closestTime));
        return closestPoint.squaredDistanceTo(player.getBoundingBox().getCenter()) <= PROJECTILE_DANGER_RADIUS_SQUARED;
    }

    private static boolean isRelevantHostile(ServerPlayerEntity player, HostileEntity hostile) {
        if (!hostile.isAlive())
            return false;

        if (hostile.getTarget() == player)
            return true;

        double distanceSquared = hostile.squaredDistanceTo(player);
        if (distanceSquared > 12.0D * 12.0D)
            return false;

        Vec3d velocity = hostile.getVelocity();
        Vec3d toPlayer = player.getPos().subtract(hostile.getPos());
        if (velocity.lengthSquared() < 0.0025D || toPlayer.lengthSquared() < 0.01D)
            return distanceSquared <= 4.0D * 4.0D;

        return velocity.normalize().dotProduct(toPlayer.normalize()) >= 0.55D;
    }

    public record Threat(int entityId, ThreatType type) {}
}
