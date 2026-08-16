package com.timelord.common.state;

import com.timelord.common.model.TemporalSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Bounded temporal histories keyed only by player UUID and dimension ID. */
public final class TimeRewindHistory {
    private final int historySize;
    private final Map<UUID, PlayerHistory> histories = new HashMap<>();

    public TimeRewindHistory(int historySize) {
        if (historySize <= 0)
            throw new IllegalArgumentException("historySize must be positive");

        this.historySize = historySize;
    }

    public void record(UUID playerId, TemporalSnapshot snapshot) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(snapshot, "snapshot");

        PlayerHistory history = histories.get(playerId);
        if (history == null || !history.dimensionId.equals(snapshot.dimensionId())) {
            history = new PlayerHistory(snapshot.dimensionId());
            histories.put(playerId, history);
        }

        history.snapshots.addLast(snapshot);
        while (history.snapshots.size() > historySize)
            history.snapshots.removeFirst();
    }

    public Optional<TemporalSnapshot> get(UUID playerId, String dimensionId, int ticksAgo) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        if (ticksAgo < 0)
            throw new IllegalArgumentException("ticksAgo must not be negative");

        PlayerHistory history = histories.get(playerId);
        if (history == null
                || !history.dimensionId.equals(dimensionId)
                || history.snapshots.size() <= ticksAgo) {
            return Optional.empty();
        }

        int indexFromNewest = 0;
        for (var iterator = history.snapshots.descendingIterator(); iterator.hasNext(); indexFromNewest++) {
            TemporalSnapshot snapshot = iterator.next();
            if (indexFromNewest == ticksAgo)
                return Optional.of(snapshot);
        }

        return Optional.empty();
    }

    public void reset(UUID playerId) {
        histories.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public void retainPlayers(Set<UUID> playerIds) {
        Objects.requireNonNull(playerIds, "playerIds");
        histories.keySet().retainAll(new HashSet<>(playerIds));
    }

    public void clear() {
        histories.clear();
    }

    private static final class PlayerHistory {
        private final String dimensionId;
        private final Deque<TemporalSnapshot> snapshots = new ArrayDeque<>();

        private PlayerHistory(String dimensionId) {
            this.dimensionId = dimensionId;
        }
    }
}
