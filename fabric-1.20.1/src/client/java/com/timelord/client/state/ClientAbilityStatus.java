package com.timelord.client.state;

import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.ClientJudgementCut;
import com.timelord.client.TimeLordClient;
import com.timelord.client.time.ClientTimeField;
import com.timelord.client.time.TheWorldClientState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ClientAbilityStatus {
    private ClientAbilityStatus() {}

    public static boolean isActive(AbilityType ability) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return false;

        return switch (ability) {
            case SLOW_TIME -> ClientTimeField.getFields().containsKey(client.player.getUuid());
            case THE_WORLD -> TheWorldClientState.canMove(client.player.getUuid());
            case DIMENSION_CUT -> ClientAbilityState.isActive(AbilityType.DIMENSION_CUT)
                    || ClientJudgementCut.isCharging();
            case TIME_SHIFT -> TimeLordClient.getTimeShiftMultiplier() > 0;
            case TIME_REWIND -> ClientTimeRewindState.getEffects().stream()
                    .anyMatch(effect -> effect.playerId().equals(client.player.getUuid()));
            case FUTURE_SIGHT -> ClientAbilityState.isActive(AbilityType.FUTURE_SIGHT);
        };
    }

    public static Text getStateText(AbilityType ability) {
        if (ability == AbilityType.THE_WORLD && isActive(ability)) {
            int ticks = TheWorldClientState.getRemainingTicks(MinecraftClient.getInstance().player.getUuid());
            return Text.translatable("screen.time-lord.book.active_remaining", formatSeconds(ticks));
        }

        if (ability == AbilityType.TIME_SHIFT && isActive(ability))
            return Text.translatable("screen.time-lord.book.active_multiplier", TimeLordClient.getTimeShiftMultiplier());

        if (ability == AbilityType.TIME_REWIND)
            return Text.translatable("screen.time-lord.book.rewind_duration", 3);

        if (ability == AbilityType.FUTURE_SIGHT && isActive(ability))
            return Text.translatable(
                    "screen.time-lord.book.active_elapsed",
                    formatSeconds(ClientAbilityState.getActiveElapsedTicks(ability))
            );

        return Text.translatable(isActive(ability)
                ? "screen.time-lord.book.active"
                : "screen.time-lord.book.inactive");
    }

    public static Text getCooldownText(AbilityType ability) {
        int remaining = ClientAbilityState.getCooldown(ability);

        if (remaining > 0)
            return Text.translatable("screen.time-lord.book.cooldown_remaining", formatSeconds(remaining));

        if (ability.cooldownTicks() <= 0)
            return Text.translatable("screen.time-lord.book.no_cooldown");

        return Text.translatable("screen.time-lord.book.cooldown", formatSeconds(ability.cooldownTicks()));
    }

    private static String formatSeconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0D);
    }
}
