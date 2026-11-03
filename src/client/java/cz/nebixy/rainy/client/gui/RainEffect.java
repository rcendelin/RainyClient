package cz.nebixy.rainy.client.gui;

import java.util.Random;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.client.util.Theme;

/**
 * Padající kapky deště v pozadí menu. Jen pár obdélníčků, takže to FPS skoro nestojí.
 */
public class RainEffect {
	private static final int DROPS = 70;

	private final Random random = new Random();
	private final float[] x = new float[DROPS];
	private final float[] y = new float[DROPS];
	private final float[] speed = new float[DROPS];
	private final int[] length = new int[DROPS];
	private final int[] alpha = new int[DROPS];
	private int width = -1;
	private int height = -1;
	private long lastFrame;

	private void reset(int i, boolean anywhere) {
		x[i] = random.nextFloat() * width;
		y[i] = anywhere ? random.nextFloat() * height : -random.nextFloat() * 40;
		speed[i] = 120 + random.nextFloat() * 160; // pixely za sekundu
		length[i] = 4 + random.nextInt(8);
		alpha[i] = 30 + random.nextInt(60);
	}

	public void render(GuiGraphicsExtractor g, int screenWidth, int screenHeight) {
		if (screenWidth != width || screenHeight != height) {
			width = screenWidth;
			height = screenHeight;
			for (int i = 0; i < DROPS; i++) {
				reset(i, true);
			}
		}

		long now = System.currentTimeMillis();
		float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
		lastFrame = now;

		for (int i = 0; i < DROPS; i++) {
			y[i] += speed[i] * dt;
			if (y[i] > height) {
				reset(i, false);
			}
			int dx = (int) x[i];
			int dy = (int) y[i];
			g.fill(dx, dy, dx + 1, dy + length[i], Theme.accent(alpha[i]));
		}
	}
}
