package com.timelord.client.sound;

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public final class TimeShiftChargeSound extends MovingSoundInstance {
    public TimeShiftChargeSound(SoundEvent soundEvent) {
        super(soundEvent, SoundCategory.PLAYERS, Random.create());

        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.relative = true;
    }

    @Override
    public void tick() {}
    public void finish() {
        setDone();
    }
}