package com.timelord.common.model;

/** A Minecraft-independent three-dimensional position or vector. */
public record TemporalPosition(double x, double y, double z) {
    public static final TemporalPosition ZERO = new TemporalPosition(0.0D, 0.0D, 0.0D);

    public TemporalPosition add(TemporalPosition other) {
        return new TemporalPosition(x + other.x, y + other.y, z + other.z);
    }

    public TemporalPosition subtract(TemporalPosition other) {
        return new TemporalPosition(x - other.x, y - other.y, z - other.z);
    }

    public TemporalPosition multiply(double scalar) {
        return new TemporalPosition(x * scalar, y * scalar, z * scalar);
    }

    public double dot(TemporalPosition other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double lengthSquared() {
        return dot(this);
    }

    public double squaredDistanceTo(TemporalPosition other) {
        return subtract(other).lengthSquared();
    }

    public TemporalPosition normalize() {
        double lengthSquared = lengthSquared();
        if (lengthSquared < 1.0E-12D)
            return ZERO;

        return multiply(1.0D / Math.sqrt(lengthSquared));
    }
}
