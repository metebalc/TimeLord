package com.timelord.client;

import com.timelord.ModParticles;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.mixin.GameRendererAccessor;
import com.timelord.client.network.AbilityClientNetworking;
import com.timelord.client.network.JudgementCutClientNetworking;
import com.timelord.client.network.TimeFieldClientNetworking;
import com.timelord.client.network.AbilityStateClientNetworking;
import com.timelord.client.network.FutureSightClientNetworking;
import com.timelord.client.network.TimeRewindClientNetworking;
import com.timelord.client.network.AbilityLoadoutClientNetworking;
import com.timelord.client.network.TheWorldClientNetworking;
import com.timelord.client.network.TimeShiftClientNetworking;
import com.timelord.client.particle.TimeShiftLightningParticle;
import com.timelord.client.render.*;
import com.timelord.client.screen.AbilityBookScreen;
import com.timelord.client.sound.TimeShiftSoundManager;
import com.timelord.client.state.ClientAbilityState;
import com.timelord.client.state.ClientFutureSightState;
import com.timelord.client.state.ClientTimeRewindState;
import com.timelord.client.state.ClientAbilityLoadoutState;
import com.timelord.client.hud.AbilityHudRenderer;
import com.timelord.client.time.ClientTimeField;
import com.timelord.client.time.TheWorldClientState;
import com.timelord.client.time.TimeShiftWaterRunner;
import com.timelord.mixin.EntityStepHeightAccessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class TimeLordClient implements ClientModInitializer {
	private static final String CATEGORY = "category.time-lord";

	private static final int SKILL_SLOT_COUNT = 3;
	private static final KeyBinding[] SKILL_KEYS = new KeyBinding[SKILL_SLOT_COUNT];
	private static KeyBinding OPEN_SKILL_MENU_KEY;
	private static KeyBinding SWITCH_SLOW_MODE_KEY;

	// CLIENT STATES
	private static final AbilityType[] ACTIVE_INPUT_ABILITIES = new AbilityType[SKILL_SLOT_COUNT];
	private static final boolean[] SKILL_KEY_HELD = new boolean[SKILL_SLOT_COUNT];
	private static Integer ACTIVE_CHARGE_SLOT;
	private static Integer TIME_SHIFT_INPUT_SLOT;


	private static boolean TIME_SHIFT_KEY_DOWN = false;
	private static boolean TIME_SHIFT_CHARGING = false;
	private static boolean TIME_SHIFT_CHARGE_PACKET_SENT = false;
	private static boolean TIME_SHIFT_BURSTING = false;

	private static int TIME_SHIFT_MULTIPLIER = 0;
	private static long TIME_SHIFT_PRESS_START_MS = 0L;
	private static int TIME_SHIFT_BURST_TICKS = 0;
	private static final int TIME_SHIFT_BURST_VISUAL_TICKS = 14;

	private static final long TIME_SHIFT_CHARGE_THRESHOLD_MS = 800L;
	private static final long TIME_SHIFT_MAX_CHARGE_MS = 1000L;

	@Override
	public void onInitializeClient() {
		SKILL_KEYS[0] = registerSkillKey("key.time-lord.use_skill_1", GLFW.GLFW_KEY_Z);
		SKILL_KEYS[1] = registerSkillKey("key.time-lord.use_skill_2", GLFW.GLFW_KEY_X);
		SKILL_KEYS[2] = registerSkillKey("key.time-lord.use_skill_3", GLFW.GLFW_KEY_C);
		OPEN_SKILL_MENU_KEY = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.time-lord.open_skill_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

		SWITCH_SLOW_MODE_KEY = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.time-lord.switch_slow_mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));

		TimeFieldClientNetworking.register();
		JudgementCutClientNetworking.register();
		AbilityStateClientNetworking.register();
		FutureSightClientNetworking.register();
		TimeRewindClientNetworking.register();
		AbilityLoadoutClientNetworking.register();

		SlowTimeFieldRenderer.register();
		JudgementCutSlashRenderer.register();
		TimeShiftRenderer.register();
		TimeRewindRenderer.register();
		FutureSightRenderer.register();
		TheWorldShockwaveRenderer.register();
		TheWorldHitRenderer.register();

		TimeShiftWaterRunner.register();
		AbilityHudRenderer.register();

		ParticleFactoryRegistry.getInstance().register(ModParticles.TIME_SHIFT_LIGHTNING, TimeShiftLightningParticle.Factory::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientTimeField.tick();
			TheWorldClientState.tick();
			ClientAbilityState.tick();
			TheWorldRenderer.tick();
			tickTimeShiftBurst();
			handleSkillMenu(client);
			handleEquippedAbilityKeys(client);
			handleSlowTimeModeSwitch(client);

			}
		);

		ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> {
					TimeShiftSoundManager.stopCharge();

					ClientTimeField.clear();
					ClientJudgementCut.clear();
					ClientAbilityState.clear();
					ClientFutureSightState.clear();
					ClientTimeRewindState.clear();
					TheWorldClientState.clear();
					ClientAbilityLoadoutState.reset();
					Arrays.fill(ACTIVE_INPUT_ABILITIES, null);
					Arrays.fill(SKILL_KEY_HELD, false);
					ACTIVE_CHARGE_SLOT = null;
					TIME_SHIFT_INPUT_SLOT = null;

					TheWorldRenderer.setActive(false);
					TheWorldHitRenderer.clear();

					TIME_SHIFT_KEY_DOWN = false;
					TIME_SHIFT_CHARGING = false;
					TIME_SHIFT_CHARGE_PACKET_SENT = false;
					TIME_SHIFT_BURSTING = false;
					TIME_SHIFT_MULTIPLIER = 0;
					TIME_SHIFT_PRESS_START_MS = 0L;
					TIME_SHIFT_BURST_TICKS = 0;
				}
		);

		AbilityClientNetworking.register();
		TheWorldClientNetworking.register();
		TimeShiftClientNetworking.register();

	}

	private static void handleSkillMenu(MinecraftClient client) {
		while (OPEN_SKILL_MENU_KEY.wasPressed()) {
			boolean skillInUse = Arrays.stream(ACTIVE_INPUT_ABILITIES).anyMatch(Objects::nonNull);
			if (client.player != null && client.currentScreen == null && !skillInUse)
				client.setScreen(new AbilityBookScreen());
		}
	}

	private static void handleEquippedAbilityKeys(MinecraftClient client) {
		if (client.player == null)
			return;

		for (int slot = 0; slot < SKILL_SLOT_COUNT; slot++) {
			boolean held = SKILL_KEYS[slot].isPressed();

			if (client.currentScreen != null) {
				SKILL_KEY_HELD[slot] = held;
				continue;
			}

			if (held && !SKILL_KEY_HELD[slot])
				startSkillInput(slot);

			if (held && TIME_SHIFT_INPUT_SLOT != null && TIME_SHIFT_INPUT_SLOT == slot)
				continueTimeShiftInput();

			if (!held && SKILL_KEY_HELD[slot])
				releaseSkillInput(slot);

			SKILL_KEY_HELD[slot] = held;
		}
	}

	private static void startSkillInput(int slot) {
		AbilityType ability = ClientAbilityLoadoutState.getEquipped(slot);
		ACTIVE_INPUT_ABILITIES[slot] = ability;

		if (ability == AbilityType.TIME_SHIFT) {
			if (TIME_SHIFT_INPUT_SLOT != null) {
				ACTIVE_INPUT_ABILITIES[slot] = null;
				return;
			}

			TIME_SHIFT_INPUT_SLOT = slot;
			startTimeShiftInput();
			return;
		}

		if (ability.isChargeable()) {
			if (ACTIVE_CHARGE_SLOT != null) {
				ACTIVE_INPUT_ABILITIES[slot] = null;
				return;
			}

			ACTIVE_CHARGE_SLOT = slot;
			startCharging(ability);
			return;
		}

		activate(ability);
	}

	private static void releaseSkillInput(int slot) {
		AbilityType ability = ACTIVE_INPUT_ABILITIES[slot];

		if (ability == AbilityType.TIME_SHIFT && TIME_SHIFT_INPUT_SLOT != null && TIME_SHIFT_INPUT_SLOT == slot) {
			releaseTimeShiftInput();
			TIME_SHIFT_INPUT_SLOT = null;
		} else if (ability != null && ability.isChargeable() && ACTIVE_CHARGE_SLOT != null && ACTIVE_CHARGE_SLOT == slot) {
			releaseCharging();
			ACTIVE_CHARGE_SLOT = null;
		}

		ACTIVE_INPUT_ABILITIES[slot] = null;
	}

	private static void startTimeShiftInput() {
		long now = System.currentTimeMillis();
		TIME_SHIFT_KEY_DOWN = true;
		TIME_SHIFT_CHARGING = false;
		TIME_SHIFT_CHARGE_PACKET_SENT = false;
		TIME_SHIFT_PRESS_START_MS = now;
	}

	private static void continueTimeShiftInput() {
		if (TIME_SHIFT_KEY_DOWN) {
			long now = System.currentTimeMillis();
			long elapsed = now - TIME_SHIFT_PRESS_START_MS;

			if (elapsed >= TIME_SHIFT_CHARGE_THRESHOLD_MS) {
				if (TIME_SHIFT_MULTIPLIER > 0) {
					if (!TIME_SHIFT_CHARGING) {
						TIME_SHIFT_CHARGING = true;
						TimeShiftSoundManager.startCharge();
					}

					if (!TIME_SHIFT_CHARGE_PACKET_SENT && TimeShiftClientNetworking.canSendStartCharge()) {
						TimeShiftClientNetworking.sendStartCharge();
						TIME_SHIFT_CHARGE_PACKET_SENT = true;
					}
				}
			}

		}
	}

	private static void releaseTimeShiftInput() {
		if (TIME_SHIFT_KEY_DOWN) {
			long now = System.currentTimeMillis();
			long elapsed = now - TIME_SHIFT_PRESS_START_MS;

			if (TIME_SHIFT_CHARGING && TIME_SHIFT_CHARGE_PACKET_SENT) {
				TimeShiftClientNetworking.sendRelease();
			} else if (elapsed < TIME_SHIFT_CHARGE_THRESHOLD_MS) {
				AbilityClientNetworking.sendActivate(AbilityType.TIME_SHIFT);
			}

			TimeShiftSoundManager.stopCharge();

			TIME_SHIFT_KEY_DOWN = false;
			TIME_SHIFT_CHARGING = false;
			TIME_SHIFT_CHARGE_PACKET_SENT = false;
			TIME_SHIFT_PRESS_START_MS = 0L;
		}
	}

	private static void activate(AbilityType ability) {
		AbilityClientNetworking.sendActivate(ability);
	}

	private static void startCharging(AbilityType ability) {
		AbilityClientNetworking.sendStartCharge(ability);
	}

	private static void releaseCharging() {
		AbilityClientNetworking.sendReleaseCharge();
	}

	public static void applyTimeShiftState(MinecraftClient client, boolean active, int multiplier) {
		TIME_SHIFT_MULTIPLIER = multiplier;

		if (!active) {
			TimeShiftSoundManager.stopCharge();
			TIME_SHIFT_KEY_DOWN = false;
			TIME_SHIFT_CHARGING = false;
			TIME_SHIFT_CHARGE_PACKET_SENT = false;
			TIME_SHIFT_MULTIPLIER = 0;
			TIME_SHIFT_PRESS_START_MS = 0L;
		}

		if (client.player == null)
			return;

		EntityStepHeightAccessor accessor = (EntityStepHeightAccessor) client.player;
		accessor.timeLord$setStepHeight(active ? 1.25F : 0.6F);
	}

	public static void applyTimeShiftBurst() {
		TimeShiftSoundManager.stopCharge();
		TIME_SHIFT_BURSTING = true;
		TIME_SHIFT_BURST_TICKS = TIME_SHIFT_BURST_VISUAL_TICKS;
		TIME_SHIFT_KEY_DOWN = false;
		TIME_SHIFT_CHARGING = false;
		TIME_SHIFT_CHARGE_PACKET_SENT = false;
		TIME_SHIFT_PRESS_START_MS = 0L;
	}

	private static void tickTimeShiftBurst() {
		if (TIME_SHIFT_BURST_TICKS <= 0)
			return;

		TIME_SHIFT_BURST_TICKS--;

		if (TIME_SHIFT_BURST_TICKS <= 0)
			TIME_SHIFT_BURSTING = false;
	}

	private static void handleSlowTimeModeSwitch(MinecraftClient client) {
		while (SWITCH_SLOW_MODE_KEY.wasPressed()) {
			if (client.player == null)
				continue;

			AbilityClientNetworking.sendSwitchSlowMode();
		}
	}

	private static KeyBinding registerSkillKey(String translationKey, int glfwKey) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, CATEGORY));
	}

	public static AbilityType getEquippedAbility(int slot) {
		return ClientAbilityLoadoutState.getEquipped(slot);
	}

	public static void equipAbility(int slot, AbilityType ability) {
		AbilityLoadoutClientNetworking.requestEquip(slot, ability);
	}

	public static String getSkillKeyName(int slot) {
		if (slot < 0 || slot >= SKILL_SLOT_COUNT)
			throw new IndexOutOfBoundsException("Invalid skill slot: " + slot);

		return SKILL_KEYS[slot].getBoundKeyLocalizedText().getString();
	}

	public static Identifier getAbilityTexture(AbilityType ability) {
		return AbilityIconRegistry.get(ability);
	}

	public static void updateMonochromeShader(MinecraftClient client) {
		if (TheWorldRenderer.isOpening())
			return;

		if (TheWorldClientState.isTimeStopped())
			return;

		if (ClientJudgementCut.isMonochrome()) {
			((GameRendererAccessor) client.gameRenderer).timeLord$loadPostProcessor(
					new Identifier("minecraft", "shaders/post/desaturate.json"));
		} else {
			client.gameRenderer.disablePostProcessor();
		}
	}

	public static boolean isTimeShiftKeyDown() {
		return TIME_SHIFT_KEY_DOWN;
	}

	public static boolean isTimeShiftCharging() {
		return TIME_SHIFT_CHARGING;
	}

	public static boolean isTimeShiftBursting() {
		return TIME_SHIFT_BURSTING;
	}

	public static int getTimeShiftMultiplier() {
		return TIME_SHIFT_MULTIPLIER;
	}

	public static float getTimeShiftHoldProgress() {
		if (!TIME_SHIFT_KEY_DOWN || TIME_SHIFT_PRESS_START_MS <= 0L)
			return 0.0F;

		long elapsed = System.currentTimeMillis() - TIME_SHIFT_PRESS_START_MS;
		return Math.min(1.0F, elapsed / (float) TIME_SHIFT_MAX_CHARGE_MS);
	}

	public static float getTimeShiftChargeProgress() {

		if (!TIME_SHIFT_CHARGING || TIME_SHIFT_PRESS_START_MS <= 0L)
			return 0.0F;

		long elapsed = System.currentTimeMillis() - TIME_SHIFT_PRESS_START_MS;
		long chargeElapsed = elapsed - TIME_SHIFT_CHARGE_THRESHOLD_MS;
		long chargeWindow = TIME_SHIFT_MAX_CHARGE_MS - TIME_SHIFT_CHARGE_THRESHOLD_MS;

		if (chargeWindow <= 0L)
			return 1.0F;

		return Math.min(1.0F, Math.max(0.0F, chargeElapsed / (float) chargeWindow));
	}

	public static boolean isTheWorldActive() {
		return TheWorldClientState.isTimeStopped();
	}
}
