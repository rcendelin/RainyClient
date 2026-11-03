package cz.nebixy.rainy.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Pomocné kreslicí funkce (zaoblené obdélníky, přepínače, text).
 */
public final class Draw {
	private Draw() {
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	public static int width(String text) {
		return font().width(text);
	}

	public static int lineHeight() {
		return font().lineHeight;
	}

	public static void text(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		g.text(font(), text, x, y, color);
	}

	public static void centeredText(GuiGraphicsExtractor g, String text, int centerX, int y, int color) {
		g.text(font(), text, centerX - width(text) / 2, y, color);
	}

	/** Text bez stínu: v menu na tmavém pozadí je ostřejší a líp čitelný. */
	public static void flat(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		g.text(font(), text, x, y, color, false);
	}

	public static void flatCentered(GuiGraphicsExtractor g, String text, int centerX, int y, int color) {
		g.text(font(), text, centerX - width(text) / 2, y, color, false);
	}

	/**
	 * Obdélník se zaoblenými rohy (radius 0-3). Kreslí se po pruzích, které se nepřekrývají,
	 * takže i poloprůhledná barva vypadá všude stejně.
	 */
	public static void rect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color, int radius) {
		if (x2 <= x1 || y2 <= y1) {
			return;
		}
		radius = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
		if (radius <= 0) {
			g.fill(x1, y1, x2, y2, color);
			return;
		}
		for (int i = 0; i < radius; i++) {
			int inset = radius - i;
			g.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
			g.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
		}
		g.fill(x1, y1 + radius, x2, y2 - radius, color);
	}

	/** Tenký rámeček (1 px) kolem obdélníku. */
	public static void border(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y1 + 1, color);
		g.fill(x1, y2 - 1, x2, y2, color);
		g.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
		g.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
	}

	/** Čárkovaný rámeček, používá ho HUD editor. */
	public static void dashedBorder(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		for (int x = x1; x < x2; x += 4) {
			g.fill(x, y1, Math.min(x + 2, x2), y1 + 1, color);
			g.fill(x, y2 - 1, Math.min(x + 2, x2), y2, color);
		}
		for (int y = y1; y < y2; y += 4) {
			g.fill(x1, y, x1 + 1, Math.min(y + 2, y2), color);
			g.fill(x2 - 1, y, x2, Math.min(y + 2, y2), color);
		}
	}

	/**
	 * Přepínač zapnuto/vypnuto. progress je 0 (vypnuto) až 1 (zapnuto) kvůli plynulé animaci.
	 */
	public static void toggle(GuiGraphicsExtractor g, int x, int y, float progress) {
		int w = 18;
		int h = 10;
		int track = Theme.lerp(Theme.SWITCH_OFF, Theme.accent(), progress);
		rect(g, x, y, x + w, y + h, track, 3);
		int knobX = x + 2 + Math.round((w - 10) * progress);
		rect(g, knobX, y + 2, knobX + 6, y + h - 2, Theme.TEXT, 1);
	}

	/** Zkrátí text, aby se vešel do šířky (přidá ...). */
	public static String trim(String text, int maxWidth) {
		if (width(text) <= maxWidth) {
			return text;
		}
		String dots = "...";
		int end = text.length();
		while (end > 0 && width(text.substring(0, end) + dots) > maxWidth) {
			end--;
		}
		return text.substring(0, end).stripTrailing() + dots;
	}

	/** Rozdělí text do řádků podle šířky. Poslední řádek se případně zkrátí. */
	public static java.util.List<String> wrap(String text, int maxWidth, int maxLines) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++) {
			String candidate = line.isEmpty() ? words[i] : line + " " + words[i];
			if (width(candidate) <= maxWidth || line.isEmpty()) {
				line.setLength(0);
				line.append(candidate);
				continue;
			}
			if (lines.size() == maxLines - 1) {
				StringBuilder rest = new StringBuilder(line);
				for (int j = i; j < words.length; j++) {
					rest.append(' ').append(words[j]);
				}
				lines.add(trim(rest.toString(), maxWidth));
				return lines;
			}
			lines.add(line.toString());
			line.setLength(0);
			line.append(words[i]);
		}
		if (!line.isEmpty()) {
			lines.add(trim(line.toString(), maxWidth));
		}
		return lines;
	}

	public static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
		return mx >= x1 && mx < x2 && my >= y1 && my < y2;
	}
}
