package cz.nebixy.rainy.client.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import cz.nebixy.rainy.client.util.Compat;

/**
 * Společný základ pro FPS Boost a Potato Graphics: po zapnutí nastaví grafiku podle předvolby,
 * po vypnutí vrátí původní nastavení. Zapnutá může být vždycky jen jedna předvolba.
 *
 * Hodnoty v předvolbě: "false", "0", "ENUM:OFF" (konstanta enumu), "MAX:6" (jen snížit na 6, nikdy nezvýšit).
 */
public abstract class SettingsPresetModule extends Module {
	private static final List<SettingsPresetModule> PRESETS = new ArrayList<>();

	private final Map<String, String> preset;
	/** Původní hodnoty, aby šly vrátit (ukládají se i do configu). */
	private final Map<String, String> original = new LinkedHashMap<>();

	protected SettingsPresetModule(String id, String name, String description, Map<String, String> preset) {
		super(id, name, description, Category.FPS, false);
		this.preset = preset;
		PRESETS.add(this);
	}

	@Override
	protected void onEnable() {
		// Druhou předvolbu nejdřív vypnout, ať vrátí svoje původní hodnoty
		for (SettingsPresetModule other : PRESETS) {
			if (other != this && other.isEnabled()) {
				other.setEnabled(false);
			}
		}
		boolean changed = false;
		for (Map.Entry<String, String> e : preset.entrySet()) {
			Object current = Compat.getOption(e.getKey());
			if (current == null) {
				continue;
			}
			Object value = target(current, e.getValue());
			if (value == null || value.equals(current)) {
				continue;
			}
			original.putIfAbsent(e.getKey(), current instanceof Enum<?> en ? en.name() : String.valueOf(current));
			changed |= Compat.setOption(e.getKey(), value);
		}
		if (changed) {
			Compat.reloadChunks();
		}
	}

	private static Object target(Object current, String spec) {
		if (spec.startsWith("ENUM:")) {
			return Compat.enumLike(current, spec.substring(5));
		}
		if (spec.startsWith("MAX:")) {
			Object max = Compat.parseLike(current, spec.substring(4));
			if (current instanceof Integer c && max instanceof Integer m) {
				return Math.min(c, m);
			}
			if (current instanceof Double c && max instanceof Double m) {
				return Math.min(c, m);
			}
			return null;
		}
		return Compat.parseLike(current, spec);
	}

	@Override
	protected void onDisable() {
		for (Map.Entry<String, String> e : original.entrySet()) {
			Object current = Compat.getOption(e.getKey());
			Object value = Compat.parseLike(current, e.getValue());
			if (value != null) {
				Compat.setOption(e.getKey(), value);
			}
		}
		original.clear();
		Compat.reloadChunks();
	}

	/** Po startu hry: předvolba je už nastavená z minula (options.txt), nic znovu neměníme. */
	@Override
	public void afterLoad() {
	}

	@Override
	public void save(JsonObject json) {
		super.save(json);
		JsonObject saved = new JsonObject();
		original.forEach(saved::addProperty);
		json.add("original", saved);
	}

	@Override
	public void load(JsonObject json) {
		super.load(json);
		original.clear();
		if (json.has("original")) {
			JsonObject saved = json.getAsJsonObject("original");
			for (String key : saved.keySet()) {
				original.put(key, saved.get(key).getAsString());
			}
		}
	}
}
