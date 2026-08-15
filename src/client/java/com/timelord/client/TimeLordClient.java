package com.timelord.client;

import com.timelord.ModParticles;
import com.timelord.TimeLord;
import com.timelord.ability.AbilityManager;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.mixin.GameRendererAccessor;
import com.timelord.client.mixin.GameRendererMixin;
import com.timelord.client.network.JudgementCutClientNetworking;
import com.timelord.client.network.TimeFieldClientNetworking;
import com.timelord.client.particle.TimeShiftLightningParticle;
import com.timelord.client.render.*;
import com.timelord.client.sound.TimeShiftSoundManager;
import com.timelord.client.time.ClientTimeField;
import com.timelord.client.time.TheWorldClientState;
import com.timelord.client.time.TimeShiftWaterRunner;
import com.timelord.mixin.EntityStepHeightAccessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class TimeLordClient implements ClientModInitializer {
	private static final String CATEGORY = "category.time-lord";

	private static final Map<AbilityType, KeyBinding> KEYS = new EnumMap<>(AbilityType.class);
	private static KeyBinding SWITCH_SLOW_MODE_KEY;

	// HUD
	private static final int RECTANGLE_WIDTH = 20;
	private static final int RECTANGLE_HEIGHT = 20;

	private static final int BASE_COLOR = 0xFF000000;

	private static final int BORDER_COLOR = 0xFFADADAD;

	private static final int TEXT_COLOR = 0xFFFFFFFF;

	private static final int HUD_LEFT_MARGIN = 10;
	private static final int HUD_BOTTOM_MARGIN = 10;
	private static final int SLOT_SPACING = 5;

	// CLIENT STATES
	private static boolean THE_WORLD_ACTIVE = false;
	private static final Map<AbilityType, Boolean> KEY_HELD = new EnumMap<>(AbilityType.class);


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

	// HUD ABILITY SLOTS
	private record AbilitySlot(AbilityType ability, String buttonText, Identifier texture) {}

	private static final Map<AbilityType, Integer> COOLDOWNS = new EnumMap<>(AbilityType.class);

	private static final Identifier SLOW_TIME_DOMAIN_TEXTURE = Identifier.of("time-lord", "textures/domain_32x32.png");
	private static final Identifier DIMENSION_CUT_TEXTURE = Identifier.of("time-lord", "textures/cuts_32x32.png");
	private static final Identifier TIME_SHIFT_TEXTURE = Identifier.of("time-lord", "textures/dash_32x32.png");
	private static final Identifier THE_WORLD_TEXTURE = Identifier.of("time-lord", "textures/hourglass_32x32.png");

	private static final AbilitySlot[] ABILITY_SLOTS = {
			new AbilitySlot(AbilityType.SLOW_TIME, "Z", SLOW_TIME_DOMAIN_TEXTURE),
			new AbilitySlot(AbilityType.THE_WORLD, "X", THE_WORLD_TEXTURE),
			new AbilitySlot(AbilityType.DIMENSION_CUT, "C", DIMENSION_CUT_TEXTURE),
			new AbilitySlot(AbilityType.TIME_SHIFT, "V", TIME_SHIFT_TEXTURE)
	};

	@Override
	public void onInitializeClient() {
		register(AbilityType.SLOW_TIME, "key.time-lord.slow_time", GLFW.GLFW_KEY_Z);
		register(AbilityType.THE_WORLD, "key.time-lord.the_world", GLFW.GLFW_KEY_X);
		register(AbilityType.DIMENSION_CUT, "key.time-lord.dimension_cut", GLFW.GLFW_KEY_C);
		register(AbilityType.TIME_SHIFT, "key.time-lord.time_shift", GLFW.GLFW_KEY_V);

		SWITCH_SLOW_MODE_KEY = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.time-lord.switch_slow_mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));

		TimeFieldClientNetworking.register();
		JudgementCutClientNetworking.register();

		SlowTimeFieldRenderer.register();
		JudgementCutSlashRenderer.register();
		TimeShiftRenderer.register();
		TheWorldShockwaveRenderer.register();
		TheWorldHitRenderer.register();

		TimeShiftWaterRunner.register();

		ParticleFactoryRegistry.getInstance().register(ModParticles.TIME_SHIFT_LIGHTNING, TimeShiftLightningParticle.Factory::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientTimeField.tick();
			TheWorldRenderer.tick();
			tickTimeShiftBurst();
			handleAbilityKeys(client);
			handleSlowTimeModeSwitch(client);

			COOLDOWNS.replaceAll((ability, cooldown) -> Math.max(0, cooldown - 1));
			}
		);

		ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> {
					TimeShiftSoundManager.stopCharge();

					ClientTimeField.clear();
					ClientJudgementCut.clear();
					KEY_HELD.clear();

					THE_WORLD_ACTIVE = false;
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

		ClientPlayNetworking.registerGlobalReceiver(
				TimeLord.COOLDOWN_PACKET,
				(client, handler, buf, responseSender) -> {
					int networkId = buf.readByte();
					int cooldown = buf.readInt();

					AbilityType ability = AbilityType.fromNetworkId(networkId);

					client.execute(() -> {
						if (ability != null) {
							COOLDOWNS.put(ability, cooldown);
						}
					});
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				TimeLord.THE_WORLD_STATE_PACKET,
				(client, handler, buf, responseSender) -> {

					int count =
							buf.readVarInt();

					Set<UUID> activeUsers =
							new LinkedHashSet<>();

					for (int i = 0; i < count; i++) {
						activeUsers.add(
								buf.readUuid()
						);
					}

					client.execute(() -> {

						boolean wasActive =
								THE_WORLD_ACTIVE;

						THE_WORLD_ACTIVE =
								!activeUsers.isEmpty();

						TheWorldClientState
								.setActiveUsers(
										activeUsers
								);

						if (!THE_WORLD_ACTIVE) {

							TheWorldRenderer
									.setActive(false);

							TheWorldShockwaveRenderer
									.clear();

							return;
						}

						if (!wasActive) {
							return;
						}
					});
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				TimeLord.THE_WORLD_HIT_PACKET,
				(client, handler, buf, responseSender) -> {

					UUID hitId =
							buf.readUuid();

					UUID targetId =
							buf.readUuid();

					UUID attackerId =
							buf.readUuid();

					Vec3d position =
							new Vec3d(
									buf.readDouble(),
									buf.readDouble(),
									buf.readDouble()
							);

					Vec3d attackDirection =
							new Vec3d(
									buf.readDouble(),
									buf.readDouble(),
									buf.readDouble()
							);

					client.execute(() ->

							TheWorldHitRenderer.addHit(
									hitId,
									targetId,
									attackerId,
									position,
									attackDirection
							)
					);
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				TimeLord.THE_WORLD_RESOLVE_PACKET,
				(client, handler, buf, responseSender) -> {

					UUID hitId =
							buf.readUuid();

					int sequenceIndex =
							buf.readVarInt();

					int totalHits =
							buf.readVarInt();

					client.execute(() ->

							TheWorldHitRenderer.resolveHit(
									hitId,
									sequenceIndex
							)
					);
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				TimeLord.THE_WORLD_ACTIVATE_PACKET,
				(client, handler, buf, responseSender) -> {

					UUID activatorId =
							buf.readUuid();

					boolean globalTransition =
							buf.readBoolean();

					client.execute(() -> {

						if (client.player == null)
							return;

						boolean localActivator =
								activatorId.equals(
										client.player
												.getUuid()
								);

						if (globalTransition) {

							if (localActivator) {

								TheWorldRenderer
										.setActive(true);

							} else {

								TheWorldRenderer
										.setRemoteActive(
												true
										);

								TheWorldShockwaveRenderer
										.start(
												activatorId
										);
							}

							return;
						}

						TheWorldShockwaveRenderer
								.start(
										activatorId
								);
					});
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				TimeLord.TIME_SHIFT_STATE_PACKET,
				(client, handler, buf, responseSender) -> {
					boolean active = buf.readBoolean();
					int multiplier = buf.readInt();

					client.execute(() -> {
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
					});
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				TimeLord.TIME_SHIFT_BURST_PACKET,
				(client, handler, buf, responseSender) -> {
					client.execute(() -> {
						TimeShiftSoundManager.stopCharge();
						TIME_SHIFT_BURSTING = true;
						TIME_SHIFT_BURST_TICKS = TIME_SHIFT_BURST_VISUAL_TICKS;
						TIME_SHIFT_KEY_DOWN = false;
						TIME_SHIFT_CHARGING = false;
						TIME_SHIFT_CHARGE_PACKET_SENT = false;
						TIME_SHIFT_PRESS_START_MS = 0L;
					});
				}
		);

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> renderHud(drawContext));
	}

	private static void handleAbilityKeys(MinecraftClient client) {
		KEYS.forEach((ability, key) -> {
			if (client.player == null)
				return;

			if (ability == AbilityType.TIME_SHIFT) {
				handleTimeShiftKey(key);
				return;
			}

			if (!ability.isChargeable()) {
				while (key.wasPressed()) {
					if (!ClientPlayNetworking.canSend(TimeLord.ACTIVATE_ABILITY_PACKET))
						continue;

					PacketByteBuf buffer = PacketByteBufs.create();

					buffer.writeByte(ability.networkId());

					ClientPlayNetworking.send(TimeLord.ACTIVATE_ABILITY_PACKET, buffer);
				}

				return;
			}

			boolean held = key.isPressed();
			boolean wasHeld = KEY_HELD.getOrDefault(ability, false);

			if (held && !wasHeld) {

				if (ClientPlayNetworking.canSend(TimeLord.START_CHARGE_PACKET)) {
					PacketByteBuf buffer = PacketByteBufs.create();
					buffer.writeByte(ability.networkId());
					ClientPlayNetworking.send(TimeLord.START_CHARGE_PACKET, buffer);
				}
			}

			if (!held && wasHeld) {
				if (ClientPlayNetworking.canSend(TimeLord.RELEASE_CHARGE_PACKET))
					ClientPlayNetworking.send(TimeLord.RELEASE_CHARGE_PACKET, PacketByteBufs.empty());
			}
			KEY_HELD.put(ability, held);
		});
	}

	private static void handleTimeShiftKey(KeyBinding key) {
		boolean held = key.isPressed();
		long now = System.currentTimeMillis();

		if (held && !TIME_SHIFT_KEY_DOWN) {
			TIME_SHIFT_KEY_DOWN = true;
			TIME_SHIFT_CHARGING = false;
			TIME_SHIFT_CHARGE_PACKET_SENT = false;
			TIME_SHIFT_PRESS_START_MS = now;
			return;
		}

		if (held && TIME_SHIFT_KEY_DOWN) {
			long elapsed = now - TIME_SHIFT_PRESS_START_MS;

			if (elapsed >= TIME_SHIFT_CHARGE_THRESHOLD_MS) {
				if (TIME_SHIFT_MULTIPLIER > 0) {
					if (!TIME_SHIFT_CHARGING) {
						TIME_SHIFT_CHARGING = true;
						TimeShiftSoundManager.startCharge();
					}

					if (!TIME_SHIFT_CHARGE_PACKET_SENT && ClientPlayNetworking.canSend(TimeLord.TIME_SHIFT_CHARGE_START_PACKET)) {
						ClientPlayNetworking.send(TimeLord.TIME_SHIFT_CHARGE_START_PACKET, PacketByteBufs.empty());
						TIME_SHIFT_CHARGE_PACKET_SENT = true;
					}
				}
			}

			return;
		}

		if (!held && TIME_SHIFT_KEY_DOWN) {
			long elapsed = now - TIME_SHIFT_PRESS_START_MS;

			if (TIME_SHIFT_CHARGING && TIME_SHIFT_CHARGE_PACKET_SENT) {
				if (ClientPlayNetworking.canSend(TimeLord.TIME_SHIFT_RELEASE_PACKET))
					ClientPlayNetworking.send(TimeLord.TIME_SHIFT_RELEASE_PACKET, PacketByteBufs.empty());
			} else if (elapsed < TIME_SHIFT_CHARGE_THRESHOLD_MS) {
				PacketByteBuf buf = PacketByteBufs.create();

				buf.writeByte(AbilityManager.AbilityType.TIME_SHIFT.networkId());

				ClientPlayNetworking.send(TimeLord.ACTIVATE_ABILITY_PACKET, buf);
			}

			TimeShiftSoundManager.stopCharge();

			TIME_SHIFT_KEY_DOWN = false;
			TIME_SHIFT_CHARGING = false;
			TIME_SHIFT_CHARGE_PACKET_SENT = false;
			TIME_SHIFT_PRESS_START_MS = 0L;
		}
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

			if (!ClientPlayNetworking.canSend(TimeLord.SWITCH_SLOW_MODE_PACKET))
				continue;

			ClientPlayNetworking.send(TimeLord.SWITCH_SLOW_MODE_PACKET, PacketByteBufs.empty());
		}
	}

	private static void renderHud(DrawContext drawContext) {
		MinecraftClient client = MinecraftClient.getInstance();

		int y = client.getWindow().getScaledHeight() - RECTANGLE_HEIGHT - HUD_BOTTOM_MARGIN;
		int countEachItem = 0;

		for (AbilitySlot slot : ABILITY_SLOTS) {
			int x = HUD_LEFT_MARGIN + countEachItem++ * (RECTANGLE_WIDTH + SLOT_SPACING);

			drawContext.fill(x, y, x + RECTANGLE_WIDTH, y + RECTANGLE_HEIGHT, BASE_COLOR);
			drawContext.drawTexture(slot.texture(), x, y, RECTANGLE_WIDTH, RECTANGLE_HEIGHT, 0, 0, 32, 32, 32, 32);
			drawContext.drawBorder(x, y, RECTANGLE_WIDTH, RECTANGLE_HEIGHT, BORDER_COLOR);
			int cooldown = COOLDOWNS.getOrDefault(slot.ability(), 0);

			if (cooldown > 0) {
				int seconds = (cooldown + 19) / 20;
				drawCenteredText(drawContext, client, Integer.toString(seconds), x, y, RECTANGLE_WIDTH, RECTANGLE_HEIGHT, true);
			} else {
				drawCenteredText(drawContext, client, slot.buttonText(), x, y, RECTANGLE_WIDTH, RECTANGLE_HEIGHT, true);
			}
		}
	}

	private static void drawCenteredText(
			DrawContext drawContext,
			MinecraftClient client,
			String text,
			int x,
			int y,
			int width,
			int height,
			boolean shadow
	) {
		int textWidth = client.textRenderer.getWidth(text);
		int textX = x + (width - textWidth) / 2;
		int textY = y + (height - client.textRenderer.fontHeight) / 2 + 1;

		drawContext.drawText(client.textRenderer, text, textX, textY, TEXT_COLOR, shadow);
	}

	private static void register(AbilityType ability, String translationKey, int glfwKey) {
		KeyBinding key = new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, CATEGORY);
		KEYS.put(ability, KeyBindingHelper.registerKeyBinding(key));
	}

	public static void updateMonochromeShader(MinecraftClient client) {
		if (TheWorldRenderer.isOpening())
			return;

		if (THE_WORLD_ACTIVE)
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
		return THE_WORLD_ACTIVE;
	}
}