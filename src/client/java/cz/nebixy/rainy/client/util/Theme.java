package cz.nebixy.rainy.client.util;

/**
 * Barvy Rainy Clientu. Všechny barvy jsou ARGB (0xAARRGGBB).
 */
public final class Theme {
	private Theme() {
	}

	/** Akcentní barvy, mezi kterými se dá v nastavení vybírat. Poslední je Duha (mění se dokola). */
	public static final int[] ACCENTS = {
			0xFF4FC3F7, // Rain Blue
			0xFF22D3EE, // Tyrkysová
			0xFF5EEAD4, // Mátová
			0xFF4ADE80, // Zelená
			0xFFA3E635, // Limetková
			0xFFFDE047, // Žlutá
			0xFFFBBF24, // Jantarová
			0xFFFB923C, // Oranžová
			0xFFF87171, // Červená
			0xFFFB7185, // Růžová
			0xFFF472B6, // Pink
			0xFFE879F9, // Fuchsie
			0xFFC084FC, // Fialová
			0xFF818CF8, // Indigo
			0xFFF1F5F9, // Sněhová
			0                // Duha
	};
	public static final String[] ACCENT_NAMES = {
			"Rain Blue", "Tyrkysová", "Mátová", "Zelená", "Limetková", "Žlutá", "Jantarová", "Oranžová",
			"Červená", "Růžová", "Pink", "Fuchsie", "Fialová", "Indigo", "Sněhová", "Duha"
	};
	public static final int RAINBOW = ACCENTS.length - 1;

	public static int accentIndex = 0;

	public static int accent() {
		int i = Math.floorMod(accentIndex, ACCENTS.length);
		return i == RAINBOW ? rainbow(0) : ACCENTS[i];
	}

	public static String accentName() {
		return ACCENT_NAMES[Math.floorMod(accentIndex, ACCENT_NAMES.length)];
	}

	/** Duhová barva, která se plynule mění v čase. offset posune odstín (0..1). */
	public static int rainbow(float offset) {
		float hue = (System.currentTimeMillis() % 6000L) / 6000f + offset;
		return 0xFF000000 | hsv(hue - (float) Math.floor(hue), 0.55f, 1f);
	}

	private static int hsv(float h, float s, float v) {
		float r, g, b;
		int i = (int) (h * 6);
		float f = h * 6 - i;
		float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
	}

	/** Barva akcentu s vlastní průhledností (0-255). */
	public static int accent(int alpha) {
		return withAlpha(accent(), alpha);
	}

	// Panely menu
	public static final int OVERLAY = 0x88060A12;
	public static final int PANEL = 0xE60E141F;
	public static final int SIDEBAR = 0xF00A0F18;
	public static final int CARD = 0xFF161E2C;
	public static final int CARD_HOVER = 0xFF1D2738;
	public static final int BORDER = 0x22FFFFFF;

	// Text
	public static final int TEXT = 0xFFF1F5F9;
	public static final int TEXT_DIM = 0xFFAAB6C8;
	public static final int TEXT_DARK = 0xFF0B1220;

	// HUD
	public static final int HUD_BG = 0x80000000;
	public static final int KEY_IDLE = 0x80000000;
	public static final int KEY_PRESSED = 0xC8FFFFFF;

	// Přepínač
	public static final int SWITCH_OFF = 0xFF2A3445;

	public static int withAlpha(int color, int alpha) {
		return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
	}

	/** Lineární přechod mezi dvěma ARGB barvami, t je 0..1. */
	public static int lerp(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = a >>> 24, ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
		int ba = b >>> 24, br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
		return (int) (aa + (ba - aa) * t) << 24
				| (int) (ar + (br - ar) * t) << 16
				| (int) (ag + (bg - ag) * t) << 8
				| (int) (ab + (bb - ab) * t);
	}
}
