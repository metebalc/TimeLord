package com.timelord;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final Identifier THE_WORLD_ID = TimeLord.id("the_world");
	public static final Identifier SLOW_TIME_ID = TimeLord.id("slow_time");
	public static final Identifier JUDGEMENT_CHARGE_ID = TimeLord.id("judgement_charge");
	public static final Identifier JUDGEMENT_RELEASE_ID = TimeLord.id("judgement_release");
	public static final Identifier JUDGEMENT_DETONATE_ID = TimeLord.id("judgement_detonate");
	public static final Identifier TIME_SHIFT_CHARGE_ID = TimeLord.id("time_shift_charge");
	public static final Identifier TIME_SHIFT_BOOM_ID = TimeLord.id("time_shift_boom");

	public static final SoundEvent THE_WORLD = SoundEvent.of(THE_WORLD_ID);
	public static final SoundEvent SLOW_TIME = SoundEvent.of(SLOW_TIME_ID);
	public static final SoundEvent JUDGEMENT_CHARGE = SoundEvent.of(JUDGEMENT_CHARGE_ID);
	public static final SoundEvent JUDGEMENT_RELEASE = SoundEvent.of(JUDGEMENT_RELEASE_ID);
	public static final SoundEvent JUDGEMENT_DETONATE = SoundEvent.of(JUDGEMENT_DETONATE_ID);
	public static final SoundEvent TIME_SHIFT_CHARGE = SoundEvent.of(TIME_SHIFT_CHARGE_ID);
	public static final SoundEvent TIME_SHIFT_BOOM = SoundEvent.of(TIME_SHIFT_BOOM_ID);

	private ModSounds() {
	}

	public static void register() {
		register(THE_WORLD_ID, THE_WORLD);
		register(SLOW_TIME_ID, SLOW_TIME);
		register(JUDGEMENT_CHARGE_ID, JUDGEMENT_CHARGE);
		register(JUDGEMENT_RELEASE_ID, JUDGEMENT_RELEASE);
		register(JUDGEMENT_DETONATE_ID, JUDGEMENT_DETONATE);
		register(TIME_SHIFT_CHARGE_ID, TIME_SHIFT_CHARGE);
		register(TIME_SHIFT_BOOM_ID, TIME_SHIFT_BOOM);
	}

	private static void register(Identifier id, SoundEvent sound) {
		Registry.register(Registries.SOUND_EVENT, id, sound);
	}
}
