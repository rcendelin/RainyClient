package cz.nebixy.rainy.client.module;

import com.google.gson.JsonObject;

import cz.nebixy.rainy.client.RainyConfig;
import cz.nebixy.rainy.client.hud.HudModule;

/**
 * Tlačítko v nastavení, které vrátí všechny HUD moduly na výchozí místo.
 */
public class ResetHudAction extends Module {
	private long clickedAt;

	public ResetHudAction() {
		super("reset_hud", "Reset HUD", "Vrátí HUD moduly na výchozí místa.", Category.SETTINGS, false);
	}

	@Override
	public void onClick() {
		for (Module m : ModuleManager.all()) {
			if (m instanceof HudModule hud) {
				hud.resetPosition();
			}
		}
		clickedAt = System.currentTimeMillis();
		RainyConfig.save();
	}

	@Override
	public String valueText() {
		return System.currentTimeMillis() - clickedAt < 1500 ? "Hotovo" : "Resetovat";
	}

	@Override
	public void save(JsonObject json) {
	}

	@Override
	public void load(JsonObject json) {
	}

	@Override
	public void afterLoad() {
	}
}
