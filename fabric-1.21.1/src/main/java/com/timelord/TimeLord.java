package com.timelord;

import com.timelord.network.ModPayloads;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeLord implements ModInitializer {
	public static final String MOD_ID = "time-lord";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModPayloads.register();
		ModSounds.register();
		ModParticles.register();
		LOGGER.info("Time Lord initialized for Minecraft 1.21.1");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
