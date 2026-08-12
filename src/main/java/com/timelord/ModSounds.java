package com.timelord;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final Identifier THE_WORLD_ID = new Identifier(TimeLord.MOD_ID, "the_world");
    public static final Identifier SLOW_TIME_ID = new Identifier(TimeLord.MOD_ID, "slow_time");
    public static final Identifier JUDGEMENT_CHARGE_ID = new Identifier(TimeLord.MOD_ID, "judgement_charge");
    public static final Identifier JUDGEMENT_RELEASE_ID = new Identifier(TimeLord.MOD_ID, "judgement_release");
    public static final Identifier JUDGEMENT_DETONATE_ID = new Identifier(TimeLord.MOD_ID, "judgement_detonate");

    public static final SoundEvent THE_WORLD = SoundEvent.of(THE_WORLD_ID);
    public static final SoundEvent SLOW_TIME = SoundEvent.of(SLOW_TIME_ID);
    public static final SoundEvent JUDGEMENT_CHARGE = SoundEvent.of(JUDGEMENT_CHARGE_ID);
    public static final SoundEvent JUDGEMENT_RELEASE = SoundEvent.of(JUDGEMENT_RELEASE_ID);
    public static final SoundEvent JUDGEMENT_DETONATE = SoundEvent.of(JUDGEMENT_DETONATE_ID);

    public static void register() {
        Registry.register(
                Registries.SOUND_EVENT,
                THE_WORLD_ID,
                THE_WORLD
        );

        Registry.register(
                Registries.SOUND_EVENT,
                SLOW_TIME_ID,
                SLOW_TIME
        );

        Registry.register(
                Registries.SOUND_EVENT,
                JUDGEMENT_CHARGE_ID,
                JUDGEMENT_CHARGE
        );

        Registry.register(
                Registries.SOUND_EVENT,
                JUDGEMENT_RELEASE_ID,
                JUDGEMENT_RELEASE
        );

        Registry.register(
                Registries.SOUND_EVENT,
                JUDGEMENT_DETONATE_ID,
                JUDGEMENT_DETONATE
        );
    }
    private ModSounds() {
    }
}