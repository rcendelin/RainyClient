package cz.nebixy.rainy.client.util;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;

/**
 * Počítá FPS a kliky za sekundu (CPS). Volá se každý snímek z HUD rendereru.
 */
public final class InputTracker {
	private InputTracker() {
	}

	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftWasDown;
	private static boolean rightWasDown;

	private static int frames;
	private static long fpsWindowStart;
	private static int fps;

	public static void onFrame() {
		long now = System.currentTimeMillis();

		// FPS
		frames++;
		if (fpsWindowStart == 0) {
			fpsWindowStart = now;
		}
		if (now - fpsWindowStart >= 1000) {
			fps = Math.round(frames * 1000f / (now - fpsWindowStart));
			frames = 0;
			fpsWindowStart = now;
		}

		// CPS
		Minecraft mc = Minecraft.getInstance();
		boolean left = mc.options.keyAttack.isDown();
		boolean right = mc.options.keyUse.isDown();
		if (left && !leftWasDown) {
			LEFT.addLast(now);
		}
		if (right && !rightWasDown) {
			RIGHT.addLast(now);
		}
		leftWasDown = left;
		rightWasDown = right;
		trim(LEFT, now);
		trim(RIGHT, now);
	}

	private static void trim(Deque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
			clicks.pollFirst();
		}
	}

	public static int fps() {
		return fps;
	}

	public static int leftCps() {
		return LEFT.size();
	}

	public static int rightCps() {
		return RIGHT.size();
	}
}
