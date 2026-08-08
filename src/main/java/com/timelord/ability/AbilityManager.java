package com.timelord.ability;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AbilityManager {
    private static final Map<AbilityType, Ability> ABILITIES = new EnumMap<>(AbilityType.class);
    private static final Map<UUID, EnumMap<AbilityType, Long>> COOLDOWNS = new HashMap<>();

    static {
        ABILITIES.put(AbilityType.SLOW_3, new SlowTimeAbility(3));
        ABILITIES.put(AbilityType.SLOW_5, new SlowTimeAbility(5));
        ABILITIES.put(AbilityType.SLOW_7, new SlowTimeAbility(7));
        ABILITIES.put(AbilityType.DIMENSION_CUT, new DimensionCutAbility());
        ABILITIES.put(AbilityType.TIME_SHIFT, new TimeShiftAbility());
    }

    private AbilityManager() {
    }

    public static void activate(ServerPlayerEntity player, int networkId) {
        AbilityType type = AbilityType.fromNetworkId(networkId);
        if (type == null || player.isSpectator() || !player.isAlive()) {
            return;
        }

        long now = player.getServerWorld().getTime();
        EnumMap<AbilityType, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(
                player.getUuid(), ignored -> new EnumMap<>(AbilityType.class));
        long readyAt = playerCooldowns.getOrDefault(type, 0L);

        if (now < readyAt) {
            double seconds = (readyAt - now) / 20.0D;
            player.sendMessage(Text.literal(String.format("Ability recharging: %.1fs", seconds)), true);
            return;
        }

        ABILITIES.get(type).activate(player);
        playerCooldowns.put(type, now + type.cooldownTicks());
        player.sendMessage(Text.literal(type.displayName()), true);
    }

    public enum AbilityType {
        SLOW_3(0, 8 * 20, "Slow Time: 3 seconds"),
        SLOW_5(1, 12 * 20, "Slow Time: 5 seconds"),
        SLOW_7(2, 16 * 20, "Slow Time: 7 seconds"),
        DIMENSION_CUT(3, 4 * 20, "Dimension Cut"),
        TIME_SHIFT(4, 14 * 20, "Time Shift");

        private final int networkId;
        private final int cooldownTicks;
        private final String displayName;

        AbilityType(int networkId, int cooldownTicks, String displayName) {
            this.networkId = networkId;
            this.cooldownTicks = cooldownTicks;
            this.displayName = displayName;
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
