package cz.nebixy.rainy.client.hud;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.InputTracker;
import cz.nebixy.rainy.client.util.Theme;

/**
 * Klávesy WASD, myš a mezerník. Zmáčknutá klávesa se rozsvítí.
 */
public class KeystrokesHud extends HudModule {
	private static final int KEY = 22;
	private static final int GAP = 2;

	/** Plynulé rozsvícení: W, A, S, D, LMB, RMB, SPACE */
	private final float[] glow = new float[7];
	private long lastFrame;

	public KeystrokesHud() {
		super("keystrokes", "Keystrokes", "WASD, myš a mezerník s CPS.", true, 4, 113);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		Options o = Minecraft.getInstance().options;
		long now = System.currentTimeMillis();
		float step = lastFrame == 0 ? 1f : Math.min(1f, (now - lastFrame) / 80f);
		lastFrame = now;

		KeyMapping[] keys = {o.keyUp, o.keyLeft, o.keyDown, o.keyRight, o.keyAttack, o.keyUse, o.keyJump};
		for (int i = 0; i < keys.length; i++) {
			float target = keys[i].isDown() ? 1f : 0f;
			glow[i] += (target - glow[i]) * step;
		}

		int full = KEY * 3 + GAP * 2;
		int half = (full - GAP) / 2;

		// W (nebo co má hráč nastavené)
		key(g, x + KEY + GAP, y, KEY, KEY, label(o.keyUp, "W"), null, glow[0]);
		// A S D
		int row2 = y + KEY + GAP;
		key(g, x, row2, KEY, KEY, label(o.keyLeft, "A"), null, glow[1]);
		key(g, x + KEY + GAP, row2, KEY, KEY, label(o.keyDown, "S"), null, glow[2]);
		key(g, x + (KEY + GAP) * 2, row2, KEY, KEY, label(o.keyRight, "D"), null, glow[3]);
		// LMB RMB
		int row3 = row2 + KEY + GAP;
		int lc = InputTracker.leftCps();
		int rc = InputTracker.rightCps();
		key(g, x, row3, half, KEY, "LMB", lc > 0 ? lc + " CPS" : null, glow[4]);
		key(g, x + half + GAP, row3, half, KEY, "RMB", rc > 0 ? rc + " CPS" : null, glow[5]);
		// Space
		int row4 = row3 + KEY + GAP;
		int spaceH = 10;
		Draw.rect(g, x, row4, x + full, row4 + spaceH, Theme.lerp(Theme.KEY_IDLE, Theme.KEY_PRESSED, glow[6]), 2);
		int barColor = Theme.lerp(Theme.TEXT, Theme.TEXT_DARK, glow[6]);
		g.fill(x + full / 2 - 10, row4 + 4, x + full / 2 + 10, row4 + 5, barColor);

		return new int[]{full, row4 + spaceH - y};
	}

	/** Krátký název klávesy (max 3 znaky, ať se vejde do políčka). */
	private static String label(KeyMapping mapping, String fallback) {
		String name = mapping.getTranslatedKeyMessage().getString().trim();
		if (name.isEmpty()) {
			return fallback;
		}
		return name.length() <= 3 ? name.toUpperCase() : name.substring(0, 3).toUpperCase();
	}

	private void key(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, String sub, float glow) {
		Draw.rect(g, x, y, x + w, y + h, Theme.lerp(Theme.KEY_IDLE, Theme.KEY_PRESSED, glow), 2);
		int textColor = glow > 0.5f ? Theme.TEXT_DARK : Theme.TEXT;
		// U myši se místo LMB/RMB ukáže číslo CPS, když klikáš (celé písmo, ať je to čitelné)
		String text = sub != null ? sub : label;
		Draw.flatCentered(g, text, x + w / 2, y + (h - 8) / 2, textColor);
	}
}
