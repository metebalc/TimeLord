package com.timelord.ability;

import com.timelord.ModSounds;
import com.timelord.TimeLord;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

import java.util.*;

public final class TheWorldAbility implements Ability {
    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();
    private static final List<PendingHit> PENDING_HITS = new ArrayList<>();
    private record PendingHit(
            UUID targetId,
            UUID attackerId,
            float damage
    ) {}

    @Override
    public void activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        MinecraftServer server = player.getServer();
        UUID playerId = player.getUuid();

        if (ACTIVE_PLAYERS.contains(playerId)) {
            ACTIVE_PLAYERS.remove(playerId);
            player.sendMessage(Text.literal("The World: OFF"),true);

            if (ACTIVE_PLAYERS.isEmpty())
                applyPendingHits(player.getServer());
        } else {
            ACTIVE_PLAYERS.add(playerId);
            player.sendMessage(Text.literal("The World: ON"), true);
            world.playSound(null, player.getBlockPos(), ModSounds.THE_WORLD,
                    SoundCategory.PLAYERS, 0.7F, 1.0F);
        }
        syncState(server);
    }

    public static boolean isTimeStopped() {
        return !ACTIVE_PLAYERS.isEmpty();
    }

    public static boolean canMove(ServerPlayerEntity player) {
        return ACTIVE_PLAYERS.contains(player.getUuid());
    }

    public static void storeHit(
            Entity target,
            ServerPlayerEntity attacker,
            float damage
    ) {
        PENDING_HITS.add(
                new PendingHit(
                        target.getUuid(),
                        attacker.getUuid(),
                        damage
                )
        );
    }

    private static void applyPendingHits(MinecraftServer server) {
        Map<UUID, Float> totalDamage = new HashMap<>();
        Map<UUID, UUID> lastAttacker = new HashMap<>();

        for (PendingHit hit : PENDING_HITS) {
            totalDamage.merge(
                    hit.targetId(),
                    hit.damage(),
                    Float::sum
            );
            lastAttacker.put(hit.targetId(), hit.attackerId());
        }

        PENDING_HITS.clear();

        for (Map.Entry<UUID, Float> entry : totalDamage.entrySet()) {
            UUID targetId = entry.getKey();
            float damage = entry.getValue();

            Entity target = findEntity(server, targetId);

            if (target == null || !target.isAlive())
                continue;

            UUID attackerId = lastAttacker.get(targetId);
            ServerPlayerEntity attacker = server.getPlayerManager().getPlayer(attackerId);

            if (attacker == null)
                continue;

            target.damage(
                    target.getDamageSources().playerAttack(attacker),
                    damage
            );
        }
    }

    private static Entity findEntity(
            MinecraftServer server,
            UUID uuid
    ) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null)
                return entity;
        }
        return null;
    }

    private static void syncState(MinecraftServer server) {
        boolean active = isTimeStopped();

        for (ServerPlayerEntity player :
                server.getPlayerManager().getPlayerList()) {

            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeBoolean(active);

            ServerPlayNetworking.send(
                    player,
                    TimeLord.THE_WORLD_STATE_PACKET,
                    buffer
            );
        }
    }

    public static void reset() {
        ACTIVE_PLAYERS.clear();
        PENDING_HITS.clear();
    }
}