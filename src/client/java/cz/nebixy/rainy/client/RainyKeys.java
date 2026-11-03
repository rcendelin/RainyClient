package cz.nebixy.rainy.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLScancode;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import cz.nebixy.rainy.RainyClient;

/**
 * Klávesy clientu. Dají se přenastavit ve hře: Možnosti, Ovládání, Rainy Client.
 */
public final class RainyKeys {
	private RainyKeys() {
	}

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(RainyClient.MOD_ID, "main"));

	public static final KeyMapping MENU = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.rainy-client.menu", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_RSHIFT, CATEGORY));

	public static final KeyMapping ZOOM = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.rainy-client.zoom", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_C, CATEGORY));

	public static final KeyMapping HUD_EDITOR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.rainy-client.hud_editor", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_H, CATEGORY));

	/** Zavolá se při startu, aby se klávesy zaregistrovaly. */
	public static void init() {
	}
}
