package cz.nebixy.rainy.client.module;

import com.google.gson.JsonObject;

import cz.nebixy.rainy.client.util.Theme;

/**
 * Barva clientu. V menu se ukáže řada barevných čtverečků, kliknutím vybereš.
 */
public class AccentColorSetting extends Module {
	public AccentColorSetting() {
		super("accent", "Barva", "Vyber si barvu clientu.", Category.SETTINGS, true);
	}

	@Override
	public void onClick() {
		Theme.accentIndex = (Theme.accentIndex + 1) % Theme.ACCENTS.length;
	}

	@Override
	public String valueText() {
		return Theme.accentName();
	}

	@Override
	public String descriptionText() {
		return "Vybraná barva: " + Theme.accentName();
	}

	public void select(int index) {
		Theme.accentIndex = Math.floorMod(index, Theme.ACCENTS.length);
	}

	@Override
	public void save(JsonObject json) {
		json.addProperty("index", Theme.accentIndex);
	}

	@Override
	public void load(JsonObject json) {
		if (json.has("index")) {
			Theme.accentIndex = json.get("index").getAsInt();
		}
	}

	@Override
	public void afterLoad() {
	}
}
