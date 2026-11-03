package cz.nebixy.rainy.client.util;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Pomůcky pro klávesy: název klávesy a jestli je zrovna zmáčknutá.
 */
public final class Keys {
	private Keys() {
	}

	/** Název klávesy, jak ho ukazuje Minecraft v Ovládání (např. "R", "Levý Alt"). */
	public static String name(int key) {
		if (key < 0) {
			return "-";
		}
		try {
			String name = InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
			return name.isBlank() ? "#" + key : name;
		} catch (Throwable t) {
			return "#" + key;
		}
	}

	public static boolean isDown(int key) {
		if (key < 0) {
			return false;
		}
		try {
			return InputConstants.isKeyDown(key);
		} catch (Throwable t) {
			return false;
		}
	}
}
