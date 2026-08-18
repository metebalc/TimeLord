package com.timelord.mih;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Pure retry queue for players who could not be restored during the reset tick. */
public final class PendingUniverseRestores {
    public static final int RETRY_INTERVAL_TICKS = 20;

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public void stage(UniverseSnapshot snapshot, long generationId) {
        entries.compute(snapshot.playerId(), (playerId, existing) -> {
            if (existing != null && existing.generationId > generationId)
                return existing;
            return new Entry(snapshot, generationId, Long.MIN_VALUE / 2L);
        });
    }

    public UniverseSnapshot beginAttempt(UUID playerId, long serverTick, boolean force) {
        Entry entry = entries.get(playerId);
        if (entry == null)
            return null;
        if (!force && serverTick - entry.lastAttemptTick < RETRY_INTERVAL_TICKS)
            return null;
        entries.put(playerId, new Entry(entry.snapshot, entry.generationId, serverTick));
        return entry.snapshot;
    }

    public void complete(UUID playerId) {
        entries.remove(playerId);
    }

    public void preserve(UUID playerId) {
        entries.remove(playerId);
    }

    public Set<UUID> playerIds() {
        return Set.copyOf(new LinkedHashSet<>(entries.keySet()));
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    private record Entry(
            UniverseSnapshot snapshot,
            long generationId,
            long lastAttemptTick
    ) {}
}
