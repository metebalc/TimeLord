package com.timelord.ability;

import com.timelord.ModSounds;
import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.adapter.TheWorldDamageAdapter;
import com.timelord.common.logic.PendingHitResolver;
import com.timelord.common.model.PendingHit;
import com.timelord.common.state.PendingHitBatch;
import com.timelord.common.state.TheWorldState;
import com.timelord.network.TheWorldNetworking;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TheWorldAbility implements Ability {

    private static final TheWorldState STATE = new TheWorldState();

    public static final int MAX_DURATION_TICKS = 10 * 20;

    private static final List<PendingHitBatch> ACTIVE_RESOLUTIONS =
            new ArrayList<>();

    private static final int DAMAGE_RESOLVE_DELAY_TICKS = 2;

    @Override
    public void activate(
            ServerPlayerEntity player
    ) {
        ServerWorld world =
                player.getServerWorld();

        MinecraftServer server =
                player.getServer();

        UUID playerId =
                player.getUuid();

        if (STATE.canMove(playerId)) {
            TheWorldState.DeactivationResult result =
                    STATE.deactivate(playerId);

            player.sendMessage(
                    Text.literal("The World: OFF"),
                    true
            );

            if (result == TheWorldState.DeactivationResult.TIME_RESUMED) {
                beginPendingHitResolution(
                        server
                );
            }

            syncState(server);

            return;
        }

        boolean globalTransition = STATE.activate(
                playerId,
                MAX_DURATION_TICKS
        );

        player.sendMessage(
                Text.literal("The World: ON"),
                true
        );

        world.playSound(
                null,
                player.getBlockPos(),
                ModSounds.THE_WORLD,
                SoundCategory.PLAYERS,
                0.7F,
                1.0F
        );

        sendActivation(
                server,
                playerId,
                globalTransition
        );

        syncState(
                server
        );
    }

    @Override
    public void tick(MinecraftServer server) {
        removeInvalidActivePlayers(server);
        tickActiveDurations(server);
        tickResolutions(server);
    }

    private static void tickActiveDurations(MinecraftServer server) {
        if (!STATE.isTimeStopped())
            return;

        List<UUID> expiredPlayers = STATE.tickDurations();
        if (expiredPlayers.isEmpty())
            return;

        for (UUID playerId : expiredPlayers) {
            ServerPlayerEntity expiredPlayer = server.getPlayerManager().getPlayer(playerId);
            if (expiredPlayer != null)
                expiredPlayer.sendMessage(Text.literal("The World: OFF"), true);
        }

        if (!STATE.isTimeStopped())
            beginPendingHitResolution(server);

        syncState(server);
    }

    public static boolean isTimeStopped() {
        return STATE.isTimeStopped();
    }

    public static boolean canMove(
            ServerPlayerEntity player
    ) {
        return STATE.canMove(
                player.getUuid()
        );
    }

    public static boolean isActiveUser(
            UUID playerId
    ) {
        return STATE.canMove(
                playerId
        );
    }

    public static Set<UUID> getActivePlayers() {
        return STATE.activeUsers();
    }

    public static void storeHit(
            Entity target,
            ServerPlayerEntity attacker,
            float damage
    ) {
        Vec3d impactPosition =
                calculateImpactPosition(
                        target,
                        attacker
                );

        Vec3d attackDirection =
                impactPosition.subtract(
                        attacker.getEyePos()
                );

        if (attackDirection.lengthSquared()
                > 0.0001D) {

            attackDirection =
                    attackDirection.normalize();

        } else {

            attackDirection =
                    attacker.getRotationVec(
                            1.0F
                    ).normalize();
        }

        PendingHit hit =
                new PendingHit(
                        UUID.randomUUID(),
                        target.getUuid(),
                        attacker.getUuid(),
                        damage,
                        TemporalPositionAdapter.fromMinecraft(impactPosition),
                        TemporalPositionAdapter.fromMinecraft(attackDirection)
                );

        STATE.storeHit(
                hit
        );

        sendStoredHitEffect(
                attacker.getServer(),
                hit
        );
    }

    private static void removeInvalidActivePlayers(
            MinecraftServer server
    ) {
        if (!STATE.isTimeStopped())
            return;

        boolean wasTimeStopped =
                STATE.isTimeStopped();

        List<UUID> removedPlayers =
                STATE.removeUsers(
                        playerId -> {
                            ServerPlayerEntity player =
                                    server
                                            .getPlayerManager()
                                            .getPlayer(
                                                    playerId
                                            );

                            return player == null
                                    || !player.isAlive();
                        }
                );

        if (removedPlayers.isEmpty())
            return;

        boolean stillTimeStopped =
                STATE.isTimeStopped();

        if (wasTimeStopped
                && !stillTimeStopped) {

            beginPendingHitResolution(
                    server
            );
        }

        syncState(
                server
        );
    }

    private static Vec3d calculateImpactPosition(
            Entity target,
            ServerPlayerEntity attacker
    ) {
        Vec3d start =
                attacker.getEyePos();

        Vec3d targetCenter =
                target.getBoundingBox()
                        .getCenter();

        double distance =
                start.distanceTo(
                        targetCenter
                ) + 1.0D;

        Vec3d direction =
                attacker.getRotationVec(
                        1.0F
                ).normalize();

        Vec3d end =
                start.add(
                        direction.multiply(
                                distance
                        )
                );

        Box searchBox =
                attacker.getBoundingBox()
                        .stretch(
                                direction.multiply(
                                        distance
                                )
                        )
                        .expand(
                                1.0D
                        );

        EntityHitResult hit =
                ProjectileUtil.raycast(
                        attacker,
                        start,
                        end,
                        searchBox,
                        entity ->
                                entity == target,
                        distance * distance
                );

        if (hit != null
                && hit.getEntity() == target) {

            return hit.getPos();
        }

        return targetCenter;
    }

    private static void beginPendingHitResolution(
            MinecraftServer server
    ) {
        List<PendingHit> hits = STATE.drainPendingHits();
        if (hits.isEmpty())
            return;

        for (int index = 0;
             index < hits.size();
             index++) {

            sendResolveHitEffect(
                    server,
                    hits.get(index),
                    index,
                    hits.size()
            );
        }

        ACTIVE_RESOLUTIONS.add(
                new PendingHitBatch(
                        hits,
                        DAMAGE_RESOLVE_DELAY_TICKS
                )
        );
    }

    private static void tickResolutions(
            MinecraftServer server
    ) {
        if (ACTIVE_RESOLUTIONS.isEmpty())
            return;

        if (isTimeStopped())
            return;

        ListIterator<PendingHitBatch> iterator =
                ACTIVE_RESOLUTIONS.listIterator();

        while (iterator.hasNext()) {

            PendingHitBatch batch =
                    iterator.next();

            if (!batch.ready()) {
                iterator.set(batch.tick());
                continue;
            }

            TheWorldDamageAdapter.apply(
                    server,
                    PendingHitResolver.resolve(batch.hits())
            );

            iterator.remove();
        }
    }

    private static void sendActivation(
            MinecraftServer server,
            UUID activatorId,
            boolean globalTransition
    ) {
        TheWorldNetworking.sendActivation(server, activatorId, globalTransition);
    }

    private static void sendStoredHitEffect(
            MinecraftServer server,
            PendingHit hit
    ) {
        TheWorldNetworking.sendStoredHit(server, hit);
    }

    private static void sendResolveHitEffect(
            MinecraftServer server,
            PendingHit hit,
            int sequenceIndex,
            int totalHits
    ) {
        TheWorldNetworking.sendResolveHit(server, hit, sequenceIndex, totalHits);
    }

    private static void syncState(
            MinecraftServer server
    ) {
        for (
                ServerPlayerEntity player
                :
                server
                        .getPlayerManager()
                        .getPlayerList()
        ) {

            syncStateTo(player);
        }
    }

    public static void syncStateTo(ServerPlayerEntity player) {
        Map<UUID, Integer> activeDurations = STATE.activeDurations();
        TheWorldNetworking.sendState(player, activeDurations, MAX_DURATION_TICKS);
    }

    public static void reset() {
        STATE.clear();
        ACTIVE_RESOLUTIONS.clear();
    }
}
