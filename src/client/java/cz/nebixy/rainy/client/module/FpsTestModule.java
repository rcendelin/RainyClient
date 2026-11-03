package cz.nebixy.rainy.client.module;

import java.util.Arrays;

import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.RainyClient;
import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.Theme;

/**
 * FPS test: po zavření menu odpočítá 3 s a pak 30 s měří každý snímek.
 * Výsledek: průměrné FPS a 1% low (průměr 1 % nejpomalejších snímků = jak moc se hra seká).
 * Stejný test jde udělat i jinde (třeba v Lunaru přes F3), ať je srovnání férové.
 */
public class FpsTestModule extends Module {
	private static final long COUNTDOWN_NS = 3_000_000_000L;
	private static final long DURATION_NS = 30_000_000_000L;
	private static final int MAX_FRAMES = 200_000; // 30 s při 6000+ FPS

	private enum State { IDLE, WAITING, COUNTDOWN, RUNNING }

	private static State state = State.IDLE;
	private static long phaseStart;
	private static long lastFrame;
	private static long[] frameTimes = new long[MAX_FRAMES];
	private static int count;

	/** Poslední výsledek (0 = ještě žádný). */
	private static int resultAvg;
	private static int resultLow;
	private static long resultShownAt;

	public FpsTestModule() {
		super("fps_test", "FPS test", "30 s změří průměrné FPS a 1% low. Během testu stůj a nehýbej se.", Category.FPS, false);
	}

	@Override
	public void onClick() {
		start();
	}

	/** "Zapnuto" = test právě běží (kvůli bublině po zmáčknutí klávesy). */
	@Override
	public boolean isEnabled() {
		return state != State.IDLE;
	}

	@Override
	public void toggle() {
		// Klávesa (bind) test spustí, nebo zruší, když zrovna běží
		if (state == State.IDLE) {
			start();
		} else {
			state = State.IDLE;
		}
	}

	private static void start() {
		state = State.WAITING; // začne, až zavřeš menu
		count = 0;
	}

	@Override
	public String valueText() {
		return switch (state) {
			case WAITING -> "Zavři menu";
			case COUNTDOWN, RUNNING -> "Měřím";
			default -> resultAvg > 0 ? resultAvg + " FPS" : "Spustit";
		};
	}

	@Override
	public String descriptionText() {
		if (state == State.IDLE && resultAvg > 0) {
			return "Minule: průměr " + resultAvg + " FPS, 1% low " + resultLow + " FPS. Klikni pro nový test.";
		}
		return description;
	}

	/** Volá se každý snímek (z HUDu). */
	public static void onFrame() {
		if (state == State.IDLE) {
			return;
		}
		long now = System.nanoTime();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			state = State.IDLE; // odešel ze hry
			return;
		}
		switch (state) {
			case WAITING -> {
				if (mc.gui.screen() == null) {
					state = State.COUNTDOWN;
					phaseStart = now;
				}
			}
			case COUNTDOWN -> {
				if (now - phaseStart >= COUNTDOWN_NS) {
					state = State.RUNNING;
					phaseStart = now;
					lastFrame = now;
					count = 0;
				}
			}
			case RUNNING -> {
				if (mc.gui.screen() != null) {
					return; // menu otevřené, ty snímky nepočítáme
				}
				if (count < MAX_FRAMES) {
					frameTimes[count++] = now - lastFrame;
				}
				lastFrame = now;
				if (now - phaseStart >= DURATION_NS) {
					finish(now - phaseStart);
				}
			}
			default -> {
			}
		}
	}

	private static void finish(long elapsedNs) {
		state = State.IDLE;
		if (count < 10) {
			return;
		}
		resultAvg = Math.round(count / (elapsedNs / 1e9f));
		long[] sorted = Arrays.copyOf(frameTimes, count);
		Arrays.sort(sorted);
		int worst = Math.max(1, count / 100);
		long sum = 0;
		for (int i = count - worst; i < count; i++) {
			sum += sorted[i];
		}
		double avgWorstNs = (double) sum / worst;
		resultLow = (int) Math.round(1e9 / avgWorstNs);
		resultShownAt = System.currentTimeMillis();
		RainyClient.LOGGER.info("[Rainy] FPS test: průměr {} FPS, 1% low {} FPS ({} snímků)", resultAvg, resultLow, count);
	}

	/** Odpočet, průběh a výsledek nahoře uprostřed. */
	public static void render(GuiGraphicsExtractor g) {
		String title;
		String detail;
		float progress = -1;
		long now = System.nanoTime();
		switch (state) {
			case COUNTDOWN -> {
				long left = (COUNTDOWN_NS - (now - phaseStart)) / 1_000_000_000L + 1;
				title = "FPS test za " + left;
				detail = "Stůj na místě a nehýbej myší";
			}
			case RUNNING -> {
				long left = (DURATION_NS - (now - phaseStart)) / 1_000_000_000L + 1;
				title = "Měřím FPS: " + left + " s";
				detail = "Stůj na místě a nehýbej myší";
				progress = Math.min(1f, (now - phaseStart) / (float) DURATION_NS);
			}
			default -> {
				if (resultAvg == 0 || System.currentTimeMillis() - resultShownAt > 12_000) {
					return;
				}
				title = "Průměr " + resultAvg + " FPS";
				detail = "1% low " + resultLow + " FPS";
			}
		}
		int w = Math.max(Draw.width(title), Draw.width(detail)) + 20;
		int x = (g.guiWidth() - w) / 2;
		int y = 28;
		Draw.rect(g, x, y, x + w, y + 30, 0xD0101724, 3);
		Draw.flatCentered(g, title, x + w / 2, y + 5, Theme.accent());
		Draw.flatCentered(g, detail, x + w / 2, y + 17, Theme.TEXT_DIM);
		if (progress >= 0) {
			g.fill(x + 3, y + 28, x + 3 + Math.round((w - 6) * progress), y + 29, Theme.accent());
		}
	}

	@Override
	public void save(JsonObject json) {
		super.save(json);
		json.addProperty("enabled", false);
		if (resultAvg > 0) {
			json.addProperty("lastAvg", resultAvg);
			json.addProperty("lastLow", resultLow);
		}
	}

	@Override
	public void load(JsonObject json) {
		super.load(json);
		if (json.has("lastAvg")) {
			resultAvg = json.get("lastAvg").getAsInt();
			resultLow = json.has("lastLow") ? json.get("lastLow").getAsInt() : 0;
		}
	}

	@Override
	public void afterLoad() {
	}
}
