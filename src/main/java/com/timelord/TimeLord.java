package com.timelord;

import com.timelord.ability.AbilityManager;
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
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerPlayNetworking.registerGlobalReceiver(ACTIVATE_ABILITY_PACKET,
				(server, player, handler, buf, responseSender) -> {
			int abilityId = buf.readUnsignedByte();
			server.execute(() -> AbilityManager.activate(player, abilityId));
				});
		ServerTickEvents.END_SERVER_TICK.register(TimeController::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TimeController.resetAll());
		LOGGER.info("TimeLord has been activated");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
