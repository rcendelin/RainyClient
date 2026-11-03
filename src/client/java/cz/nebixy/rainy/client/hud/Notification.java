package cz.nebixy.rainy.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.Theme;

/**
 * Malá bublina nahoře uprostřed, když modul zapneš/vypneš klávesou.
 */
public final class Notification {
	private Notification() {
	}

	private static String text;
	private static boolean on;
	private static long shownAt;

	public static void show(String moduleName, boolean enabled) {
		text = moduleName;
		on = enabled;
		shownAt = System.currentTimeMillis();
	}

	public static void render(GuiGraphicsExtractor g) {
		if (text == null) {
			return;
		}
		long age = System.currentTimeMillis() - shownAt;
		if (age > 1500) {
			return;
		}
		String state = on ? "ZAPNUTO" : "VYPNUTO";
		int w = Draw.width(text) + 8 + Draw.width(state) + 16;
		int x = (g.guiWidth() - w) / 2;
		int y = 6;
		Draw.rect(g, x, y, x + w, y + 17, 0xD0101724, 3);
		g.fill(x + 3, y + 16, x + w - 3, y + 17, on ? Theme.accent() : 0xFF6B7280);
		Draw.flat(g, text, x + 8, y + 5, Theme.TEXT);
		Draw.flat(g, state, x + 8 + Draw.width(text) + 8, y + 5, on ? Theme.accent() : 0xFF9CA3AF);
	}
}
