package com.timelord;

import com.timelord.ability.AbilityManager;
import com.timelord.ability.SlowTimeAbility;
import com.timelord.ability.TimeShiftAbility;
import com.timelord.ability.TheWorldAbility;
import com.timelord.ability.FutureSightAbility;
import com.timelord.ability.AbilityLoadoutManager;
import com.timelord.network.AbilityLoadoutNetworking;
import com.timelord.time.TimeController;
import com.timelord.rewind.PlayerStateHistory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.util.Identifier;

public final class TimeLord implements ModInitializer {
	public static final String MOD_ID = "time-lord";

	public static final Identifier ACTIVATE_ABILITY_PACKET = new Identifier(MOD_ID, "activate_ability");
	public static final Identifier START_CHARGE_PACKET = new Identifier(MOD_ID, "start_charge");
	public static final Identifier RELEASE_CHARGE_PACKET = new Identifier(MOD_ID, "release_charge");
	public static final Identifier SWITCH_SLOW_MODE_PACKET = new Identifier(MOD_ID, "switch_slow_mode");
	public static final Identifier COOLDOWN_PACKET = new Identifier(MOD_ID, "cooldown");

	public static final Identifier THE_WORLD_STATE_PACKET = new Identifier(MOD_ID, "the_world_state");
	public static final Identifier THE_WORLD_HIT_PACKET = new Identifier(MOD_ID, "the_world_hit");
	public static final Identifier THE_WORLD_RESOLVE_PACKET = new Identifier(MOD_ID, "the_world_resolve");
	public static final Identifier THE_WORLD_ACTIVATE_PACKET = new Identifier(MOD_ID, "the_world_activate");

	public static final Identifier TIME_SHIFT_STATE_PACKET = new Identifier(MOD_ID, "time_shift_state");
	public static final Identifier TIME_SHIFT_CHARGE_START_PACKET = new Identifier(MOD_ID, "time_shift_charge_start");
	public static final Identifier TIME_SHIFT_RELEASE_PACKET = new Identifier(MOD_ID, "time_shift_release");
	public static final Identifier TIME_SHIFT_BURST_PACKET = new Identifier(MOD_ID, "time_shift_burst");

	public static final Identifier JUDGEMENT_VISUAL_START_PACKET = new Identifier(MOD_ID, "judgement_visual_start");
	public static final Identifier JUDGEMENT_VISUAL_RELEASE_PACKET = new Identifier(MOD_ID, "judgement_visual_release");
	public static final Identifier JUDGEMENT_VISUAL_CLEAR_PACKET = new Identifier(MOD_ID, "judgement_visual_clear");
	public static final Identifier JUDGEMENT_MONOCHROME_PACKET = new Identifier(MOD_ID, "judgement_monochrome");

	@Override
	public void onInitialize() {
		ModSounds.register();
		ModParticles.register();
		registerAbilityNetworking();
		registerTimeShiftNetworking();
		registerServerTick();
		AbilityLoadoutNetworking.registerServerReceiver();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			server.execute(() -> {
				AbilityLoadoutManager.sync(handler.player);
				AbilityManager.syncCooldowns(handler.player);
				TheWorldAbility.syncStateTo(handler.player);
			});
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			AbilityManager.cancelCharging(handler.player);
			FutureSightAbility.clear(handler.player.getUuid());
			TimeShiftAbility.cancelTransientState(handler.player);
		});
	}

	private static void registerAbilityNetworking() {
		ServerPlayNetworking.registerGlobalReceiver(
				ACTIVATE_ABILITY_PACKET,
				(
						server,
						player,
						handler,
						buf,
						responseSender
				) -> {
					int networkId = buf.readByte();
					server.execute(() -> {
						AbilityManager.activate(player, networkId);
					});
				}
		);

		ServerPlayNetworking.registerGlobalReceiver(
				START_CHARGE_PACKET,
				(
						server,
						player,
						handler,
						buf,
						responseSender
				) -> {

					int networkId = buf.readByte();
					server.execute(() -> {
						AbilityManager.startCharging(player, networkId);
					});
				}
		);

		ServerPlayNetworking.registerGlobalReceiver(
				RELEASE_CHARGE_PACKET,
				(
						server,
						player,
						handler,
						buf,
						responseSender
				) -> {
					server.execute(() -> {
						AbilityManager.releaseCharging(player);
					});
				}
		);

		ServerPlayNetworking.registerGlobalReceiver(
				SWITCH_SLOW_MODE_PACKET,
				(
						server,
						player,
						handler,
						buf,
						responseSender
				) -> {
					server.execute(() -> {
						SlowTimeAbility.switchMode(player);
					});
				}
		);
	}

	private static void registerTimeShiftNetworking() {
		ServerPlayNetworking.registerGlobalReceiver(
				TIME_SHIFT_CHARGE_START_PACKET,
				(
						server,
						player,
						handler,
						buf,
						responseSender
				) -> {

					server.execute(() -> {
						if (AbilityLoadoutManager.isEquipped(player, AbilityManager.AbilityType.TIME_SHIFT))
							TimeShiftAbility.startLaunchCharge(player);
					});
				}
		);

		ServerPlayNetworking.registerGlobalReceiver(
				TIME_SHIFT_RELEASE_PACKET,
				(
						server,
						player,
						handler,
						buf,
						responseSender
				) -> {

					server.execute(() -> {
						TimeShiftAbility.release(player);
					});
				}
		);
	}

	private static void registerServerTick() {
		ServerTickEvents.END_SERVER_TICK.register(
				server -> {
					PlayerStateHistory.tick(server);
					TimeController.tick(server);
					AbilityManager.tick(server);
				}
		);
	}

}
