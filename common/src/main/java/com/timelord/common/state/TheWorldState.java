package com.timelord.common.state;

import com.timelord.common.model.PendingHit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Ordered state for The World, with no entity, world, effect, or network dependencies. */
public final class TheWorldState {
    private final LinkedHashMap<UUID, Integer> activeUsers = new LinkedHashMap<>();
    private final List<PendingHit> pendingHits = new ArrayList<>();

    /** @return whether this activation began a global stopped-time transition */
    public boolean activate(UUID playerId, int durationTicks) {
        Objects.requireNonNull(playerId, "playerId");
        if (durationTicks <= 0)
            throw new IllegalArgumentException("durationTicks must be positive");

        boolean globalTransition = activeUsers.isEmpty();
        activeUsers.put(playerId, durationTicks);
        return globalTransition;
    }

    public DeactivationResult deactivate(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (activeUsers.remove(playerId) == null)
            return DeactivationResult.NOT_ACTIVE;

        return activeUsers.isEmpty()
                ? DeactivationResult.TIME_RESUMED
                : DeactivationResult.STILL_STOPPED;
    }

    /** Decrements active durations and returns expired users in activation order. */
    public List<UUID> tickDurations() {
        List<UUID> expired = new ArrayList<>();

        var iterator = activeUsers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                expired.add(entry.getKey());
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        return List.copyOf(expired);
    }

    /** Removes users matching the supplied invalid-user predicate. */
    public List<UUID> removeUsers(Predicate<UUID> invalidUser) {
        Objects.requireNonNull(invalidUser, "invalidUser");
        List<UUID> removed = new ArrayList<>();

        var iterator = activeUsers.keySet().iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            if (invalidUser.test(playerId)) {
                removed.add(playerId);
                iterator.remove();
            }
        }

        return List.copyOf(removed);
    }

    public boolean isTimeStopped() {
        return !activeUsers.isEmpty();
    }

    public boolean canMove(UUID playerId) {
        return activeUsers.containsKey(playerId);
    }

    public Set<UUID> activeUsers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(activeUsers.keySet()));
    }

    public Map<UUID, Integer> activeDurations() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(activeUsers));
    }

    public int remainingTicks(UUID playerId) {
        return activeUsers.getOrDefault(playerId, 0);
    }

    public void storeHit(PendingHit hit) {
        pendingHits.add(Objects.requireNonNull(hit, "hit"));
    }

    public List<PendingHit> pendingHits() {
        return List.copyOf(pendingHits);
    }

    public List<PendingHit> drainPendingHits() {
        List<PendingHit> drained = List.copyOf(pendingHits);
        pendingHits.clear();
        return drained;
    }

    public void clear() {
        activeUsers.clear();
        pendingHits.clear();
    }

    public enum DeactivationResult {
        NOT_ACTIVE,
        STILL_STOPPED,
        TIME_RESUMED
    }
}
