package com.timelord.client.state;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class ClientTimeRewindState {
    private static final List<RewindEffect> EFFECTS = new ArrayList<>();

    private ClientTimeRewindState() {}

    public static void start(UUID playerId, Vec3d origin, Vec3d destination, int durationTicks) {
        EFFECTS.add(new RewindEffect(playerId, origin, destination, durationTicks, durationTicks));
    }

    public static void tick() {
        Iterator<RewindEffect> iterator = EFFECTS.iterator();

        while (iterator.hasNext()) {
            RewindEffect effect = iterator.next();
            int remaining = effect.remainingTicks() - 1;

            if (remaining <= 0) {
                iterator.remove();
            } else {
                effect.remainingTicks = remaining;
            }
        }
    }

    public static List<RewindEffect> getEffects() {
        return List.copyOf(EFFECTS);
    }

    public static void clear() {
        EFFECTS.clear();
    }

    public static final class RewindEffect {
        private final UUID playerId;
        private final Vec3d origin;
        private final Vec3d destination;
        private final int totalTicks;
        private int remainingTicks;

        private RewindEffect(UUID playerId, Vec3d origin, Vec3d destination, int totalTicks, int remainingTicks) {
            this.playerId = playerId;
            this.origin = origin;
            this.destination = destination;
            this.totalTicks = totalTicks;
            this.remainingTicks = remainingTicks;
        }

        public UUID playerId() { return playerId; }
        public Vec3d origin() { return origin; }
        public Vec3d destination() { return destination; }
        public int totalTicks() { return totalTicks; }
        public int remainingTicks() { return remainingTicks; }
    }
}
