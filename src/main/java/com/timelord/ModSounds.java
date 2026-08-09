package com.timelord;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final Identifier THE_WORLD_ID = new Identifier(TimeLord.MOD_ID, "the_world");
    public static final SoundEvent THE_WORLD = SoundEvent.of(THE_WORLD_ID);
    public static void register() {
        Registry.register(
                Registries.SOUND_EVENT,
                THE_WORLD_ID,
                THE_WORLD
        );
    }
    private ModSounds() {
    }
}