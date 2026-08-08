package com.timelord.client;

import com.timelord.TimeLord;
import com.timelord.ability.AbilityManager.AbilityType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

public final class TimeLordClient implements ClientModInitializer {
	private static final String CATEGORY = "category.time-lord";
	private static final Map<AbilityType, KeyBinding> KEYS = new EnumMap<>(AbilityType.class);

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		register(AbilityType.SLOW_3, "key.time-lord.slow_3", GLFW.GLFW_KEY_Z);
		register(AbilityType.SLOW_5, "key.time-lord.slow_5", GLFW.GLFW_KEY_X);
		register(AbilityType.SLOW_7, "key.time-lord.slow_7", GLFW.GLFW_KEY_C);
		register(AbilityType.DIMENSION_CUT, "key.time-lord.dimension_cut", GLFW.GLFW_KEY_V);
		register(AbilityType.TIME_SHIFT, "key.time-lord.time_shift", GLFW.GLFW_KEY_B);

		ClientTickEvents.END_CLIENT_TICK.register(client -> KEYS.forEach((ability, key) -> {
			while (key.wasPressed()) {
				if (client.player != null && ClientPlayNetworking.canSend(TimeLord.ACTIVATE_ABILITY_PACKET)) {
					PacketByteBuf buffer = PacketByteBufs.create();
					buffer.writeByte(ability.networkId());
					ClientPlayNetworking.send(TimeLord.ACTIVATE_ABILITY_PACKET, buffer);
				}
			}
		}));
	}

	private static void register(AbilityType ability, String translationKey, int glfwKey) {
		KeyBinding key = new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, CATEGORY);
		KEYS.put(ability, KeyBindingHelper.registerKeyBinding(key));
	}
}
