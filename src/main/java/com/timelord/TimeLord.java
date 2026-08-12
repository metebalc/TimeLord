package com.timelord;

import com.timelord.ability.AbilityManager;
import com.timelord.ability.JudgementCutAbility;
import com.timelord.ability.SlowTimeAbility;
import com.timelord.ability.TheWorldAbility;
import com.timelord.time.TimeController;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeLord implements ModInitializer {
	public static final String MOD_ID = "time-lord";
	public static final Identifier ACTIVATE_ABILITY_PACKET = id("activate_ability");
	public static final Identifier COOLDOWN_PACKET = new Identifier("time-lord", "cooldown");
	public static final Identifier SWITCH_SLOW_MODE_PACKET = id("switch_slow_mode");
	public static final Identifier THE_WORLD_STATE_PACKET = id("the_world_state");
	public static final Identifier START_CHARGE_PACKET = id("start_charge");
	public static final Identifier RELEASE_CHARGE_PACKET = id("release_charge");

	public static final Identifier JUDGEMENT_VISUAL_START_PACKET = id("judgement_visual_start");
	public static final Identifier JUDGEMENT_VISUAL_RELEASE_PACKET = id("judgement_visual_release");
	public static final Identifier JUDGEMENT_VISUAL_CLEAR_PACKET = id("judgement_visual_clear");
	public static final Identifier JUDGEMENT_MONOCHROME_PACKET = id("judgement_monochrome");

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ModSounds.register();
		ServerPlayNetworking.registerGlobalReceiver(ACTIVATE_ABILITY_PACKET,
				(server, player, handler, buf, responseSender) -> {
			int abilityId = buf.readUnsignedByte();
			server.execute(() -> AbilityManager.activate(player, abilityId));
		});

		ServerPlayNetworking.registerGlobalReceiver(SWITCH_SLOW_MODE_PACKET,
				(server, player, handler, buf, responseSender) -> {
					server.execute(() -> SlowTimeAbility.switchMode(player));
				}
		);

		ServerPlayNetworking.registerGlobalReceiver(
				START_CHARGE_PACKET,
				(server, player, handler, buf, responseSender) -> {

					int abilityId =
							buf.readUnsignedByte();

					server.execute(() ->
							AbilityManager.startCharging(
									player,
									abilityId
							)
					);
				}
		);

		ServerPlayNetworking.registerGlobalReceiver(
				RELEASE_CHARGE_PACKET,
				(server, player, handler, buf, responseSender) -> {

					server.execute(() ->
							AbilityManager.releaseCharging(player)
					);
				}
		);

		ServerTickEvents.END_SERVER_TICK.register(
				server -> {
					TimeController.tick(server);
					AbilityManager.tick(server);
				}
		);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TimeController.resetAll(server);
			TheWorldAbility.reset();
		});
		LOGGER.info("TimeLord has been activated");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
