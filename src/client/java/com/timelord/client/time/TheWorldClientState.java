package com.timelord.client.time;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class TheWorldClientState {

    private static final Set<UUID> ACTIVE_USERS =
            new LinkedHashSet<>();

    private TheWorldClientState() {
    }

    public static void setActiveUsers(
            Set<UUID> users
    ) {
        ACTIVE_USERS.clear();
        ACTIVE_USERS.addAll(
                users
        );
    }

    public static boolean isTimeStopped() {
        return !ACTIVE_USERS.isEmpty();
    }

    public static boolean canMove(
            UUID playerId
    ) {
        return ACTIVE_USERS.contains(
                playerId
        );
    }

    public static Set<UUID> getActiveUsers() {
        return Collections.unmodifiableSet(
                ACTIVE_USERS
        );
    }

    public static void clear() {
        ACTIVE_USERS.clear();
    }
}