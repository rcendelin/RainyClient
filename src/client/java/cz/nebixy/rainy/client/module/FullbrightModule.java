package cz.nebixy.rainy.client.module;

import com.google.gson.JsonObject;

import cz.nebixy.rainy.client.util.Compat;

/**
 * Fullbright: světlo všude, i v noci a v jeskyních, jako s Night Vision.
 * Samotné světlo zapíná LightmapMixin, tady je jen vypínač.
 */
public class FullbrightModule extends Module {
	/** Čte ho LightmapMixin každý tick. */
	public static volatile boolean active;

	/** Jas uložený starou verzí Fullbrightu (když existuje), ať ho můžeme vrátit. */
	private static Double legacyOriginal;

	public FullbrightModule() {
		super("fullbright", "Fullbright", "Světlo všude, i v noci a v jeskyni (jako Night Vision).", Category.VISUAL, false);
	}

	@Override
	protected void onEnable() {
		active = true;
		fixOldBrightness();
	}

	@Override
	protected void onDisable() {
		active = false;
	}

	@Override
	public void afterLoad() {
		active = isEnabled();
		fixOldBrightness();
	}

	/**
	 * Stará verze Fullbrightu nastavovala jas na 1600 %. Když tam pořád je, vrátíme ho na 100 %.
	 */
	private static void fixOldBrightness() {
		Object gamma = Compat.getOption("gamma");
		if (gamma instanceof Double d && d > 1.0) {
			Compat.setOptionRaw("gamma", legacyOriginal != null && legacyOriginal <= 1.0 ? legacyOriginal : 1.0);
		}
	}

	@Override
	public void load(JsonObject json) {
		super.load(json);
		if (json.has("original")) {
			legacyOriginal = json.get("original").getAsDouble();
		}
	}

	/** Volá se při vypnutí hry. Jas už neměníme, takže není co vracet. */
	public void restoreForShutdown() {
		fixOldBrightness();
	}
}
