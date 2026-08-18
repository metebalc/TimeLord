package com.timelord.mih;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PendingUniverseRestoresTest {
    @Test
    void retriesAreBoundedButJoinAndRespawnCanForceAnAttempt() {
        UUID playerId = UUID.randomUUID();
        UniverseSnapshot snapshot = snapshot(playerId, 1.0D);
        PendingUniverseRestores restores = new PendingUniverseRestores();
        restores.stage(snapshot, 1L);

        assertEquals(snapshot, restores.beginAttempt(playerId, 100L, false));
        assertNull(restores.beginAttempt(playerId, 119L, false));
        assertEquals(snapshot, restores.beginAttempt(playerId, 119L, true));
        assertEquals(snapshot, restores.beginAttempt(playerId, 139L, false));
    }

    @Test
    void newerResetSupersedesAnOlderPendingSnapshotWithoutDuplication() {
        UUID playerId = UUID.randomUUID();
        PendingUniverseRestores restores = new PendingUniverseRestores();
        restores.stage(snapshot(playerId, 1.0D), 1L);
        UniverseSnapshot newer = snapshot(playerId, 9.0D);
        restores.stage(newer, 2L);

        assertEquals(1, restores.size());
        assertEquals(newer, restores.beginAttempt(playerId, 0L, true));
        restores.complete(playerId);
        assertEquals(0, restores.size());
    }

    private static UniverseSnapshot snapshot(UUID playerId, double x) {
        return new UniverseSnapshot(
                playerId, "minecraft:overworld", x, 64.0D, 2.0D,
                0.0F, 0.0F, 0.0D, 0.0D, 0.0D, 20.0F
        );
    }
}
