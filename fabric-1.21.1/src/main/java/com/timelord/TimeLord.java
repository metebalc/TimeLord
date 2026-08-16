package com.timelord;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeLord implements ModInitializer {
	public static final String MOD_ID = "time-lord";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Time Lord initialized for Minecraft 1.21.1");
	}
}
