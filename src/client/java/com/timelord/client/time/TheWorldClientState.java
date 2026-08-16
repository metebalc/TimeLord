package com.timelord.client.time;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TheWorldClientState {

    private static final Map<UUID, Integer> ACTIVE_USERS =
            new LinkedHashMap<>();

    private static int maxDurationTicks;

    private TheWorldClientState() {
    }

    public static void setActiveUsers(
            Map<UUID, Integer> users,
            int totalDurationTicks
    ) {
        ACTIVE_USERS.clear();
        ACTIVE_USERS.putAll(
                users
        );
        maxDurationTicks = totalDurationTicks;
    }

    public static void tick() {
        ACTIVE_USERS.replaceAll((playerId, remaining) -> Math.max(0, remaining - 1));
    }

    public static boolean isTimeStopped() {
        return !ACTIVE_USERS.isEmpty();
    }

    public static boolean canMove(
            UUID playerId
    ) {
        return ACTIVE_USERS.containsKey(
                playerId
        );
    }

    public static Set<UUID> getActiveUsers() {
        return Collections.unmodifiableSet(ACTIVE_USERS.keySet());
    }

    public static int getRemainingTicks(UUID playerId) {
        return ACTIVE_USERS.getOrDefault(playerId, 0);
    }

    public static int getLongestRemainingTicks() {
        return ACTIVE_USERS.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public static int getMaxDurationTicks() {
        return maxDurationTicks;
    }

    public static void clear() {
        ACTIVE_USERS.clear();
        maxDurationTicks = 0;
    }
}
