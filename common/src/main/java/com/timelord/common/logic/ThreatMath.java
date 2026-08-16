package com.timelord.common.logic;

import com.timelord.common.model.TemporalPosition;

import java.util.Objects;

/** Pure calculations used by Future Sight after Minecraft has supplied candidate snapshots. */
public final class ThreatMath {
    public static final double PROJECTILE_LOOKAHEAD_TICKS = 40.0D;
    public static final double PROJECTILE_DANGER_RADIUS_SQUARED = 2.25D;
    public static final double HOSTILE_RELEVANCE_RADIUS_SQUARED = 12.0D * 12.0D;
    public static final double IMMEDIATE_HOSTILE_RADIUS_SQUARED = 4.0D * 4.0D;
    public static final double HOSTILE_APPROACH_DOT_THRESHOLD = 0.55D;

    private static final double MIN_MOVEMENT_SPEED_SQUARED = 0.0025D;

    private ThreatMath() {}

    public static boolean isDangerousProjectile(
            TemporalPosition playerCenter,
            TemporalPosition projectilePosition,
            TemporalPosition projectileVelocity
    ) {
        Objects.requireNonNull(playerCenter, "playerCenter");
        Objects.requireNonNull(projectilePosition, "projectilePosition");
        Objects.requireNonNull(projectileVelocity, "projectileVelocity");

        double speedSquared = projectileVelocity.lengthSquared();
        if (speedSquared < MIN_MOVEMENT_SPEED_SQUARED)
            return false;

        TemporalPosition toPlayer = playerCenter.subtract(projectilePosition);
        double closestTime = toPlayer.dot(projectileVelocity) / speedSquared;
        if (closestTime <= 0.0D || closestTime > PROJECTILE_LOOKAHEAD_TICKS)
            return false;

        TemporalPosition closestPoint = projectilePosition.add(projectileVelocity.multiply(closestTime));
        return closestPoint.squaredDistanceTo(playerCenter) <= PROJECTILE_DANGER_RADIUS_SQUARED;
    }

    public static boolean isRelevantHostile(
            TemporalPosition playerPosition,
            TemporalPosition hostilePosition,
            TemporalPosition hostileVelocity,
            boolean targetingPlayer
    ) {
        Objects.requireNonNull(playerPosition, "playerPosition");
        Objects.requireNonNull(hostilePosition, "hostilePosition");
        Objects.requireNonNull(hostileVelocity, "hostileVelocity");

        if (targetingPlayer)
            return true;

        double distanceSquared = hostilePosition.squaredDistanceTo(playerPosition);
        if (distanceSquared > HOSTILE_RELEVANCE_RADIUS_SQUARED)
            return false;

        TemporalPosition toPlayer = playerPosition.subtract(hostilePosition);
        if (hostileVelocity.lengthSquared() < MIN_MOVEMENT_SPEED_SQUARED || toPlayer.lengthSquared() < 0.01D)
            return distanceSquared <= IMMEDIATE_HOSTILE_RADIUS_SQUARED;

        return hostileVelocity.normalize().dot(toPlayer.normalize()) >= HOSTILE_APPROACH_DOT_THRESHOLD;
    }
}
