package com.timelord.client.sound;

import com.timelord.ModSounds;

import net.minecraft.client.MinecraftClient;

public final class TimeShiftSoundManager {
    private static TimeShiftChargeSound chargeSound;

    private TimeShiftSoundManager() {}

    public static void startCharge() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (chargeSound != null && client.getSoundManager().isPlaying(chargeSound))
            return;

        chargeSound = new TimeShiftChargeSound(ModSounds.TIME_SHIFT_CHARGE);
        client.getSoundManager().play(chargeSound);
    }

    public static void stopCharge() {
        if (chargeSound == null)
            return;

        MinecraftClient client = MinecraftClient.getInstance();

        chargeSound.finish();
        client.getSoundManager().stop(chargeSound);
        chargeSound = null;
    }

}