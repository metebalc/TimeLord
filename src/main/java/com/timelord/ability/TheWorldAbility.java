package com.timelord.ability;

import com.timelord.ModSounds;
import com.timelord.TimeLord;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TheWorldAbility implements Ability {

    private static final Set<UUID> ACTIVE_PLAYERS =
            new LinkedHashSet<>();

    private static final Map<UUID, Integer> ACTIVE_DURATIONS =
            new LinkedHashMap<>();

    public static final int MAX_DURATION_TICKS = 10 * 20;

    private static final List<PendingHit> PENDING_HITS =
            new ArrayList<>();

    private static final List<ResolutionBatch> ACTIVE_RESOLUTIONS =
            new ArrayList<>();

    private static final int DAMAGE_RESOLVE_DELAY_TICKS = 2;

    private static final class ResolutionBatch {

        private final List<PendingHit> hits;

        private int ticksRemaining;

        private ResolutionBatch(
                List<PendingHit> hits
        ) {
            this.hits = hits;
            this.ticksRemaining =
                    DAMAGE_RESOLVE_DELAY_TICKS;
        }
    }

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

        if (ACTIVE_PLAYERS.contains(playerId)) {
            ACTIVE_PLAYERS.remove(playerId);
            ACTIVE_DURATIONS.remove(playerId);

            player.sendMessage(
                    Text.literal("The World: OFF"),
                    true
            );

            if (ACTIVE_PLAYERS.isEmpty()) {
                beginPendingHitResolution(
                        server
                );
            }

            syncState(server);

            return;
        }

        boolean globalTransition =
                ACTIVE_PLAYERS.isEmpty();

        ACTIVE_PLAYERS.add(
                playerId
        );

        ACTIVE_DURATIONS.put(
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
        if (ACTIVE_PLAYERS.isEmpty())
            return;

        boolean wasTimeStopped = true;
        boolean changed = false;
        Iterator<Map.Entry<UUID, Integer>> iterator = ACTIVE_DURATIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                ServerPlayerEntity expiredPlayer = server.getPlayerManager().getPlayer(entry.getKey());
                if (expiredPlayer != null)
                    expiredPlayer.sendMessage(Text.literal("The World: OFF"), true);

                ACTIVE_PLAYERS.remove(entry.getKey());
                iterator.remove();
                changed = true;
            } else {
                entry.setValue(remaining);
            }
        }

        if (!changed)
            return;

        if (wasTimeStopped && ACTIVE_PLAYERS.isEmpty())
            beginPendingHitResolution(server);

        syncState(server);
    }

    public static boolean isTimeStopped() {
        return !ACTIVE_PLAYERS.isEmpty();
    }

    public static boolean canMove(
            ServerPlayerEntity player
    ) {
        return ACTIVE_PLAYERS.contains(
                player.getUuid()
        );
    }

    public static boolean isActiveUser(
            UUID playerId
    ) {
        return ACTIVE_PLAYERS.contains(
                playerId
        );
    }

    public static Set<UUID> getActivePlayers() {
        return Set.copyOf(
                ACTIVE_PLAYERS
        );
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
                        impactPosition,
                        attackDirection
                );

        PENDING_HITS.add(
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
        if (ACTIVE_PLAYERS.isEmpty())
            return;

        boolean wasTimeStopped =
                !ACTIVE_PLAYERS.isEmpty();

        boolean changed =
                ACTIVE_PLAYERS.removeIf(
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

        ACTIVE_DURATIONS.keySet().retainAll(ACTIVE_PLAYERS);

        if (!changed)
            return;

        boolean stillTimeStopped =
                !ACTIVE_PLAYERS.isEmpty();

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
        if (PENDING_HITS.isEmpty())
            return;

        List<PendingHit> hits =
                new ArrayList<>(
                        PENDING_HITS
                );

        PENDING_HITS.clear();

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
                new ResolutionBatch(
                        hits
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

        Iterator<ResolutionBatch> iterator =
                ACTIVE_RESOLUTIONS.iterator();

        while (iterator.hasNext()) {

            ResolutionBatch batch =
                    iterator.next();

            if (batch.ticksRemaining > 0) {
                batch.ticksRemaining--;
                continue;
            }

            applyResolutionDamage(
                    server,
                    batch.hits
            );

            iterator.remove();
        }
    }

    private static void applyResolutionDamage(
            MinecraftServer server,
            List<PendingHit> hits
    ) {
        Map<UUID, Map<UUID, Float>>
                damageByTargetAndAttacker =
                new LinkedHashMap<>();

        Map<UUID, PendingHit>
                lastHitByTarget =
                new HashMap<>();

        for (PendingHit hit : hits) {

            damageByTargetAndAttacker
                    .computeIfAbsent(
                            hit.targetId(),
                            ignored ->
                                    new LinkedHashMap<>()
                    )
                    .merge(
                            hit.attackerId(),
                            hit.damage(),
                            Float::sum
                    );

            lastHitByTarget.put(
                    hit.targetId(),
                    hit
            );
        }

        for (
                Map.Entry<
                        UUID,
                        Map<UUID, Float>
                        > targetEntry
                :
                damageByTargetAndAttacker
                        .entrySet()
        ) {

            Entity target =
                    findEntity(
                            server,
                            targetEntry.getKey()
                    );

            if (target == null
                    || !target.isAlive()) {

                continue;
            }

            for (
                    Map.Entry<UUID, Float>
                            attackerEntry
                    :
                    targetEntry
                            .getValue()
                            .entrySet()
            ) {

                ServerPlayerEntity attacker =
                        server
                                .getPlayerManager()
                                .getPlayer(
                                        attackerEntry
                                                .getKey()
                                );

                if (attacker == null)
                    continue;

                target.timeUntilRegen = 0;

                target.damage(
                        target
                                .getDamageSources()
                                .playerAttack(
                                        attacker
                                ),
                        attackerEntry
                                .getValue()
                );

                if (!target.isAlive())
                    break;
            }

            PendingHit lastHit =
                    lastHitByTarget.get(
                            targetEntry.getKey()
                    );

            if (lastHit != null
                    && target.isAlive()) {

                applyFinalKnockback(
                        target,
                        lastHit.attackDirection()
                );

                sendFinalResolveEffect(
                        server,
                        target
                );
            }
        }
    }

    private static void applyFinalKnockback(
            Entity target,
            Vec3d attackDirection
    ) {
        Vec3d horizontal =
                new Vec3d(
                        attackDirection.x,
                        0.0D,
                        attackDirection.z
                );

        if (horizontal.lengthSquared()
                < 0.0001D) {

            return;
        }

        horizontal =
                horizontal.normalize();

        Vec3d velocity =
                target.getVelocity();

        target.setVelocity(
                velocity.x
                        + horizontal.x
                        * 0.65D,

                Math.max(
                        velocity.y,
                        0.22D
                ),

                velocity.z
                        + horizontal.z
                        * 0.65D
        );

        target.velocityModified =
                true;
    }

    private static void sendActivation(
            MinecraftServer server,
            UUID activatorId,
            boolean globalTransition
    ) {
        for (
                ServerPlayerEntity player
                :
                server
                        .getPlayerManager()
                        .getPlayerList()
        ) {

            PacketByteBuf buffer =
                    PacketByteBufs.create();

            buffer.writeUuid(
                    activatorId
            );

            buffer.writeBoolean(
                    globalTransition
            );

            ServerPlayNetworking.send(
                    player,
                    TimeLord.THE_WORLD_ACTIVATE_PACKET,
                    buffer
            );
        }
    }

    private static void sendStoredHitEffect(
            MinecraftServer server,
            PendingHit hit
    ) {
        for (
                ServerPlayerEntity player
                :
                server
                        .getPlayerManager()
                        .getPlayerList()
        ) {

            PacketByteBuf buf =
                    PacketByteBufs.create();

            buf.writeUuid(
                    hit.hitId()
            );

            buf.writeUuid(
                    hit.targetId()
            );

            buf.writeUuid(
                    hit.attackerId()
            );

            buf.writeDouble(
                    hit.impactPosition().x
            );

            buf.writeDouble(
                    hit.impactPosition().y
            );

            buf.writeDouble(
                    hit.impactPosition().z
            );

            buf.writeDouble(
                    hit.attackDirection().x
            );

            buf.writeDouble(
                    hit.attackDirection().y
            );

            buf.writeDouble(
                    hit.attackDirection().z
            );

            ServerPlayNetworking.send(
                    player,
                    TimeLord.THE_WORLD_HIT_PACKET,
                    buf
            );
        }
    }

    private static void sendResolveHitEffect(
            MinecraftServer server,
            PendingHit hit,
            int sequenceIndex,
            int totalHits
    ) {
        for (
                ServerPlayerEntity player
                :
                server
                        .getPlayerManager()
                        .getPlayerList()
        ) {

            PacketByteBuf buf =
                    PacketByteBufs.create();

            buf.writeUuid(
                    hit.hitId()
            );

            buf.writeVarInt(
                    sequenceIndex
            );

            buf.writeVarInt(
                    totalHits
            );

            ServerPlayNetworking.send(
                    player,
                    TimeLord.THE_WORLD_RESOLVE_PACKET,
                    buf
            );
        }
    }

    private static void sendFinalResolveEffect(
            MinecraftServer server,
            Entity target
    ) {
        ServerWorld world =
                (ServerWorld)
                        target.getWorld();

        world.spawnParticles(
                net.minecraft.particle
                        .ParticleTypes.CRIT,
                target.getX(),
                target.getBodyY(
                        0.55D
                ),
                target.getZ(),
                18,
                0.35D,
                0.55D,
                0.35D,
                0.18D
        );
    }

    private static Entity findEntity(
            MinecraftServer server,
            UUID uuid
    ) {
        for (
                ServerWorld world
                :
                server.getWorlds()
        ) {

            Entity entity =
                    world.getEntity(
                            uuid
                    );

            if (entity != null)
                return entity;
        }

        return null;
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
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(ACTIVE_PLAYERS.size());

        for (UUID activePlayerId : ACTIVE_PLAYERS) {
            buffer.writeUuid(activePlayerId);
            buffer.writeVarInt(ACTIVE_DURATIONS.getOrDefault(activePlayerId, 0));
        }

        buffer.writeVarInt(MAX_DURATION_TICKS);
        ServerPlayNetworking.send(player, TimeLord.THE_WORLD_STATE_PACKET, buffer);
    }

    public static void reset() {
        ACTIVE_PLAYERS.clear();
        ACTIVE_DURATIONS.clear();
        PENDING_HITS.clear();
        ACTIVE_RESOLUTIONS.clear();
    }

    private record PendingHit(
            UUID hitId,
            UUID targetId,
            UUID attackerId,
            float damage,
            Vec3d impactPosition,
            Vec3d attackDirection
    ) {
    }
}
