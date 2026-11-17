package com.fairypegasus.client;

import com.fairypegasus.FairyPegasusMod;
import com.fairypegasus.FlightInput;
import com.fairypegasus.entity.FairyPegasusEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class FairyPegasusClient implements ClientModInitializer {
	private static KeyBinding flyUpKey;
	private static KeyBinding flyDownKey;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(FairyPegasusMod.FAIRY_PEGASUS, FairyPegasusRenderer::new);

		// Default keys: R = up, C = down. Players can change them in Controls.
		flyUpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.fairypegasus.fly_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.fairypegasus"));
		flyDownKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.fairypegasus.fly_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.fairypegasus"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			int vertical = 0;
			if (client.player != null && client.player.getVehicle() instanceof FairyPegasusEntity) {
				if (flyUpKey.isPressed()) {
					vertical += 1;
				}
				if (flyDownKey.isPressed()) {
					vertical -= 1;
				}
			}
			FlightInput.vertical = vertical;
		});
	}
}
