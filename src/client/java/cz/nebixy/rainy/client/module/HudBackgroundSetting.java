package cz.nebixy.rainy.client.module;

import cz.nebixy.rainy.client.hud.HudModule;

public class HudBackgroundSetting extends Module {
	public HudBackgroundSetting() {
		super("hud_background", "Pozadí HUD", "Tmavý box pod HUD moduly.", Category.SETTINGS, true);
	}

	@Override
	protected void onEnable() {
		HudModule.background = true;
	}

	@Override
	protected void onDisable() {
		HudModule.background = false;
	}

	@Override
	public void afterLoad() {
		HudModule.background = isEnabled();
	}
}
