package com.timelord;

import com.timelord.ability.AbilityManager;
import com.timelord.ability.TimeShiftAbility;
import com.timelord.ability.TheWorldAbility;
import com.timelord.ability.FutureSightAbility;
import com.timelord.ability.AbilityLoadoutManager;
import com.timelord.network.AbilityLoadoutNetworking;
import com.timelord.network.AbilityNetworking;
import com.timelord.network.TimeShiftNetworking;
import com.timelord.common.network.PacketChannels;
import com.timelord.time.TimeController;
import com.timelord.rewind.PlayerStateHistory;
import com.timelord.mih.MadeInHeavenServerController;
import com.timelord.mih.MadeInHeavenPhysicalController;
import com.timelord.mih.TemporalActionController;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class TimeLord implements ModInitializer {
	public static final String MOD_ID = PacketChannels.MOD_ID;

	@Override
	public void onInitialize() {
		ModSounds.register();
		ModParticles.register();
		AbilityNetworking.registerServerReceivers();
		TimeShiftNetworking.registerServerReceivers();
		registerServerTick();
		AbilityLoadoutNetworking.registerServerReceiver();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			server.execute(() -> {
				PlayerStateHistory.reset(handler.player.getUuid());
				AbilityLoadoutManager.sync(handler.player);
				AbilityManager.syncCooldowns(handler.player);
				TimeShiftAbility.restorePersistentState(handler.player);
				TheWorldAbility.syncStateTo(handler.player);
				MadeInHeavenServerController.onJoin(handler.player);
			});
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			MadeInHeavenServerController.onDisconnect(server, handler.player);
			PlayerStateHistory.reset(handler.player.getUuid());
			AbilityManager.cancelCharging(handler.player);
			FutureSightAbility.clear(handler.player.getUuid());
			TimeShiftAbility.cancelTransientState(handler.player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerStateHistory.reset(newPlayer.getUuid());
			TimeShiftAbility.restorePersistentState(newPlayer);
			TheWorldAbility.syncStateTo(newPlayer);
			MadeInHeavenServerController.onRespawn(oldPlayer, newPlayer, alive);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			TimeShiftAbility.syncState(player);
			TheWorldAbility.syncStateTo(player);
			MadeInHeavenServerController.onDimensionChanged(player, origin, destination);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			MadeInHeavenPhysicalController.clear(server);
			TemporalActionController.clear();
			TimeController.clear();
			MadeInHeavenServerController.reset();
		});
	}

	private static void registerServerTick() {
		ServerTickEvents.END_SERVER_TICK.register(
				server -> {
					PlayerStateHistory.tick(server);
					TimeController.tick(server);
					MadeInHeavenServerController.tick(server);
					MadeInHeavenPhysicalController.tick(server);
					TemporalActionController.tick(server);
					AbilityManager.tick(server);
				}
		);
	}

}
