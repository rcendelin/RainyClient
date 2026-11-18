package cz.nebixy.rainy.client.module;

import com.google.gson.JsonObject;

import cz.nebixy.rainy.client.util.Compat;

/**
 * Jedna věc v PvP Style. Buď změní nastavení Minecraftu (a po vypnutí ho vrátí),
 * nebo jen zapne příznak, který čte mixin (Low Fire).
 */
public class PvpOption extends Module {
	public final PvpStyleModule parent;
	private final String option;
	private final Object onValue;
	/** Původní hodnota nastavení, aby šla vrátit (ukládá se do configu). */
	private String original;
	private Runnable onChange;

	/** Možnost, která mění nastavení Minecraftu (option = název, onValue = hodnota při zapnutí). */
	public PvpOption(PvpStyleModule parent, String id, String name, String description, String option, Object onValue) {
		super("pvp_" + id, name, description, Category.PVP, false);
		this.parent = parent;
		this.option = option;
		this.onValue = onValue;
	}

	/** Možnost bez nastavení Minecraftu, jen zavolá onChange (třeba Low Fire). */
	public PvpOption(PvpStyleModule parent, String id, String name, String description, Runnable onChange) {
		this(parent, id, name, description, null, null);
		this.onChange = onChange;
	}

	@Override
	public boolean bindable() {
		return false;
	}

	@Override
	protected void onEnable() {
		if (option != null) {
			Object current = Compat.getOption(option);
			if (current != null && !current.equals(onValue)) {
				if (original == null) {
					original = String.valueOf(current);
				}
				Compat.setOption(option, onValue);
			}
		}
		if (onChange != null) {
			onChange.run();
		}
	}

	@Override
	protected void onDisable() {
		if (option != null && original != null) {
			Object current = Compat.getOption(option);
			Object value = Compat.parseLike(current, original);
			if (value != null) {
				Compat.setOption(option, value);
			}
			original = null;
		}
		if (onChange != null) {
			onChange.run();
		}
	}

	/** Po startu: nastavení Minecraftu už je uložené v options.txt, jen obnovíme příznaky. */
	@Override
	public void afterLoad() {
		if (onChange != null) {
			onChange.run();
		}
	}

	@Override
	public void save(JsonObject json) {
		super.save(json);
		if (original != null) {
			json.addProperty("original", original);
		}
	}

	@Override
	public void load(JsonObject json) {
		super.load(json);
		original = json.has("original") ? json.get("original").getAsString() : null;
	}
}
