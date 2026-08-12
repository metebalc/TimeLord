package com.timelord.client;

import com.timelord.TimeLord;
import com.timelord.ability.AbilityManager.AbilityType;
import com.timelord.client.mixin.GameRendererMixin;
import com.timelord.client.network.JudgementCutClientNetworking;
import com.timelord.client.network.TimeFieldClientNetworking;
import com.timelord.client.render.JudgementCutSlashRenderer;
import com.timelord.client.render.SlowTimeFieldRenderer;
import com.timelord.client.time.ClientTimeField;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

public final class TimeLordClient implements ClientModInitializer {
	private static final String CATEGORY = "category.time-lord";
	private static final Map<AbilityType, KeyBinding> KEYS = new EnumMap<>(AbilityType.class);
	private static KeyBinding SWITCH_SLOW_MODE_KEY;

	private static final int RECTANGLE_WIDTH = 20;
	private static final int RECTANGLE_HEIGHT = 20;
	private static final int BASE_COLOR = 0xFF000000;
	private static final int BORDER_COLOR = 0XFFADADAD;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int HUD_LEFT_MARGIN = 10;
	private static final int HUD_BOTTOM_MARGIN = 10;
	private static final int SLOT_SPACING = 5;

	private static boolean THE_WORLD_ACTIVE = false;
	private static final Map<AbilityType, Boolean> KEY_HELD = new EnumMap<>(AbilityType.class);

	private record AbilitySlot(AbilityType ability, String buttonText, Identifier texture) {};

	private static final Map<AbilityType, Integer> COOLDOWNS = new EnumMap<>(AbilityType.class);
	private static final Identifier SLOW_TIME_DOMAIN_TEXTURE = Identifier.of("time-lord", "textures/domain_32x32.png");
	private static final Identifier DIMENSION_CUT_TEXTURE = Identifier.of("time-lord", "textures/cuts_32x32.png");
	private static final Identifier TIME_SHIFT_TEXTURE = Identifier.of("time-lord", "textures/dash_32x32.png");
	private static final Identifier THE_WORLD_TEXTURE = Identifier.of("time-lord", "textures/hourglass_32x32.png");

	private static final AbilitySlot[] ABILITY_SLOTS = {
			new AbilitySlot(AbilityType.SLOW_TIME, "Z", SLOW_TIME_DOMAIN_TEXTURE),
			new AbilitySlot(AbilityType.THE_WORLD, "X", THE_WORLD_TEXTURE),
			new AbilitySlot(AbilityType.DIMENSION_CUT, "C", DIMENSION_CUT_TEXTURE),
			new AbilitySlot(AbilityType.TIME_SHIFT, "V",  TIME_SHIFT_TEXTURE),
	};

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

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		register(AbilityType.SLOW_TIME, "key.time-lord.slow_time", GLFW.GLFW_KEY_Z);
		register(AbilityType.THE_WORLD, "key.time-lord.the_world", GLFW.GLFW_KEY_X);
		register(AbilityType.DIMENSION_CUT, "key.time-lord.dimension_cut", GLFW.GLFW_KEY_C);
		register(AbilityType.TIME_SHIFT, "key.time-lord.time_shift", GLFW.GLFW_KEY_V);
		SWITCH_SLOW_MODE_KEY = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.time-lord.switch_slow_mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY)
				);

		TimeFieldClientNetworking.register();
		JudgementCutClientNetworking.register();

		SlowTimeFieldRenderer.register();
		JudgementCutSlashRenderer.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientTimeField.tick();
			KEYS.forEach((ability, key) -> {
				if (client.player == null)
					return;

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
					if (ClientPlayNetworking.canSend(TimeLord.RELEASE_CHARGE_PACKET)) {
						ClientPlayNetworking.send(TimeLord.RELEASE_CHARGE_PACKET, PacketByteBufs.empty());
					}
				}
				KEY_HELD.put(ability, held);
			});
			while (SWITCH_SLOW_MODE_KEY.wasPressed()) {
				if (client.player != null && ClientPlayNetworking.canSend(TimeLord.SWITCH_SLOW_MODE_PACKET)) {
					ClientPlayNetworking.send(TimeLord.SWITCH_SLOW_MODE_PACKET, PacketByteBufs.empty());
				}
			}

			COOLDOWNS.replaceAll((ability, cooldown) -> Math.max(0, cooldown - 1)
			);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
					ClientTimeField.clear();
					ClientJudgementCut.clear();
					KEY_HELD.clear();
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
					boolean active = buf.readBoolean();

					client.execute(() -> {
						THE_WORLD_ACTIVE = active;
						updateMonochromeShader(client);
					});
				}
		);

		HudRenderCallback.EVENT.register(((drawContext, tickDelta) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			int y = client.getWindow().getScaledHeight() - RECTANGLE_HEIGHT - HUD_BOTTOM_MARGIN;
			int countEachItem = 0;

			for (AbilitySlot slot : ABILITY_SLOTS) {
				int x = HUD_LEFT_MARGIN  + (countEachItem++) * (RECTANGLE_WIDTH + SLOT_SPACING);

				drawContext.fill(x, y, x + RECTANGLE_WIDTH, y + RECTANGLE_HEIGHT, BASE_COLOR);
				drawContext.drawTexture(
						slot.texture(),
						x,
						y,
						RECTANGLE_WIDTH,
						RECTANGLE_HEIGHT,
						0,
						0,
						32,
						32,
						32,
						32
				);
				drawContext.drawBorder(x, y,  RECTANGLE_WIDTH, RECTANGLE_HEIGHT, BORDER_COLOR);

				int cooldown = COOLDOWNS.getOrDefault(slot.ability(), 0);

				if (cooldown > 0) {
					int seconds = (cooldown+19) / 20;
					drawCenteredText(drawContext, client, Integer.toString(seconds), x, y, RECTANGLE_WIDTH, RECTANGLE_HEIGHT, true);
				}
				else {
					drawCenteredText(drawContext, client, slot.buttonText(), x, y, RECTANGLE_WIDTH, RECTANGLE_HEIGHT, true);
				}
			}
		}));
	}

	private static void register(AbilityType ability, String translationKey, int glfwKey) {
		KeyBinding key = new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, CATEGORY);
		KEYS.put(ability, KeyBindingHelper.registerKeyBinding(key));
	}

	public static void updateMonochromeShader(MinecraftClient client) {
		boolean monochrome = THE_WORLD_ACTIVE || ClientJudgementCut.isMonochrome();

		if (monochrome) {
			((GameRendererMixin) client.gameRenderer).timeLord$loadPostProcessor(
					new Identifier("minecraft", "shaders/post/desaturate.json"));
		} else {
			client.gameRenderer.disablePostProcessor();
		}
	}
}