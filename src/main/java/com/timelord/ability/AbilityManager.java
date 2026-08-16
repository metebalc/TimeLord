package com.timelord.ability;

import com.timelord.TimeLord;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AbilityManager {

    private static final Map<AbilityType, Ability> ABILITIES = new EnumMap<>(AbilityType.class);
    private static final Map<UUID, EnumMap<AbilityType, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, AbilityType> ACTIVE_CHARGES = new HashMap<>();

    static {
        ABILITIES.put(AbilityType.SLOW_TIME, new SlowTimeAbility(10));
        ABILITIES.put(AbilityType.THE_WORLD, new TheWorldAbility());
        ABILITIES.put(AbilityType.DIMENSION_CUT, new JudgementCutAbility());
        ABILITIES.put(AbilityType.TIME_SHIFT, new TimeShiftAbility());
        ABILITIES.put(AbilityType.TIME_REWIND, new TimeRewindAbility());
        ABILITIES.put(AbilityType.FUTURE_SIGHT, new FutureSightAbility());
    }

    private AbilityManager() {}

    public static void activate(ServerPlayerEntity player, int networkId) {
        AbilityType type = AbilityType.fromNetworkId(networkId);

        if (!canUseAbility(player, type))
            return;

        Ability ability = ABILITIES.get(type);

        if (ability == null)
            return;

        if (!AbilityLoadoutManager.isEquipped(player, type))
            return;

        if (ability instanceof ChargeableAbility)
            return;

        if (ability instanceof ToggleableAbility toggleable) {
            if (toggleable.isActive(player)) {
                if (toggleable.tryActivate(player))
                    startCooldown(player, type);

                return;
            }

            if (isOnCooldown(player, type))
                return;

            toggleable.tryActivate(player);
            return;
        }

        if (type == AbilityType.TIME_SHIFT || type == AbilityType.THE_WORLD) {
            ability.activate(player);
            return;
        }

        if (isOnCooldown(player, type))
            return;

        boolean activated;

        if (ability instanceof ConditionalAbility conditional) {
            activated = conditional.tryActivate(player);
        } else {
            ability.activate(player);
            activated = true;
        }

        if (!activated)
            return;

        startCooldown(player, type);

        player.sendMessage(Text.translatable(type.translationKey()), true);
    }

    public static void startCharging(ServerPlayerEntity player, int networkId) {
        AbilityType type = AbilityType.fromNetworkId(networkId);

        if (!canUseAbility(player, type))
            return;

        Ability ability = ABILITIES.get(type);

        if (!(ability instanceof ChargeableAbility chargeable))
            return;

        if (!AbilityLoadoutManager.isEquipped(player, type))
            return;

        if (ACTIVE_CHARGES.containsKey(player.getUuid()))
            return;

        if (isOnCooldown(player, type))
            return;

        boolean started = chargeable.startCharging(player);

        if (!started)
            return;

        ACTIVE_CHARGES.put(player.getUuid(), type);
    }

    public static void releaseCharging(ServerPlayerEntity player) {
        AbilityType type = ACTIVE_CHARGES.remove(player.getUuid());

        if (type == null)
            return;

        Ability ability = ABILITIES.get(type);

        if (!(ability instanceof ChargeableAbility chargeable))
            return;

        boolean released = chargeable.release(player);

        if (!released)
            return;

        startCooldown(player, type);

        player.sendMessage(Text.translatable(type.translationKey()), true);
    }

    public static void cancelCharging(ServerPlayerEntity player) {
        AbilityType type = ACTIVE_CHARGES.remove(player.getUuid());

        if (type == null)
            return;

        Ability ability = ABILITIES.get(type);

        if (ability instanceof ChargeableAbility chargeable)
            chargeable.cancelCharging(player);
    }

    public static void tick(MinecraftServer server) {
        for (Ability ability : ABILITIES.values()) {
            ability.tick(server);
        }
        Map<UUID, AbilityType> activeCharges = new HashMap<>(ACTIVE_CHARGES);

        for (Map.Entry<UUID, AbilityType> entry : activeCharges.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null)
                continue;

            if (!player.isAlive() || player.isSpectator()) {
                cancelCharging(player);
                continue;
            }
            Ability ability = ABILITIES.get(entry.getValue());

            if (ability instanceof ChargeableAbility chargeable)
                chargeable.tickCharging(player);
        }
    }

    public static boolean isCharging(ServerPlayerEntity player) {
        return ACTIVE_CHARGES.containsKey(player.getUuid());
    }

    public static void deactivateIfActive(ServerPlayerEntity player, AbilityType type) {
        Ability ability = ABILITIES.get(type);

        if (ability instanceof ToggleableAbility toggleable && toggleable.isActive(player)) {
            if (toggleable.tryActivate(player))
                startCooldown(player, type);
        }
    }

    private static boolean canUseAbility(ServerPlayerEntity player, AbilityType type) {
        return type != null && !player.isSpectator() && player.isAlive();
    }

    private static boolean isOnCooldown(ServerPlayerEntity player, AbilityType type) {
        long now = player.getServerWorld().getTime();
        EnumMap<AbilityType, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), ignored -> new EnumMap<>(AbilityType.class));
        long readyAt = playerCooldowns.getOrDefault(type, 0L);

        if (now >= readyAt)
            return false;

        double seconds = (readyAt - now) / 20.0D;

        player.sendMessage(Text.literal(String.format("Ability recharging: %.1fs", seconds)), true);
        return true;
    }

    private static void startCooldown(ServerPlayerEntity player, AbilityType type) {
        if (type.cooldownTicks() <= 0)
            return;

        long now = player.getServerWorld().getTime();
        EnumMap<AbilityType, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), ignored -> new EnumMap<>(AbilityType.class));

        playerCooldowns.put(type, now + type.cooldownTicks());

        PacketByteBuf buffer = PacketByteBufs.create();

        buffer.writeByte(type.networkId());

        buffer.writeInt(type.cooldownTicks());

        ServerPlayNetworking.send(player, TimeLord.COOLDOWN_PACKET, buffer);
    }

    public static void syncCooldowns(ServerPlayerEntity player) {
        EnumMap<AbilityType, Long> playerCooldowns = COOLDOWNS.get(player.getUuid());
        if (playerCooldowns == null)
            return;

        long now = player.getServerWorld().getTime();

        for (Map.Entry<AbilityType, Long> entry : playerCooldowns.entrySet()) {
            int remaining = (int) Math.max(0L, entry.getValue() - now);
            if (remaining <= 0)
                continue;

            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeByte(entry.getKey().networkId());
            buffer.writeInt(remaining);
            ServerPlayNetworking.send(player, TimeLord.COOLDOWN_PACKET, buffer);
        }
    }

    public enum AbilityType {
        SLOW_TIME(0, 8 * 20, "Slow Time", false),
        THE_WORLD(1, 0, "The World", false),
        DIMENSION_CUT(2, 4 * 20, "The Judgement Cut", true),
        TIME_SHIFT(3, 0, "Time Shift", false),
        TIME_REWIND(4, 15 * 20, "Time Rewind", false),
        FUTURE_SIGHT(5, 20 * 20, "Future Sight", false);

        private final int networkId;
        private final int cooldownTicks;
        private final String displayName;
        private final boolean chargeable;

        AbilityType(int networkId, int cooldownTicks, String displayName, boolean chargeable) {
            this.networkId = networkId;
            this.cooldownTicks = cooldownTicks;
            this.displayName = displayName;
            this.chargeable = chargeable;
        }

        public int networkId() {
            return networkId;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public String displayName() {
            return displayName;
        }

        public String translationKey() {
            return switch (this) {
                case SLOW_TIME -> "ability.time-lord.slow_time";
                case THE_WORLD -> "ability.time-lord.the_world";
                case DIMENSION_CUT -> "ability.time-lord.dimension_cut";
                case TIME_SHIFT -> "ability.time-lord.time_shift";
                case TIME_REWIND -> "ability.time-lord.time_rewind";
                case FUTURE_SIGHT -> "ability.time-lord.future_sight";
            };
        }

        public boolean isChargeable() {
            return chargeable;
        }

        public static AbilityType fromNetworkId(int id) {
            for (AbilityType type : values()) {
                if (type.networkId == id) {
                    return type;
                }
            }

            return null;
        }
    }
}
