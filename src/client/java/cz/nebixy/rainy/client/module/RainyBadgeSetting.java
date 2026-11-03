package cz.nebixy.rainy.client.module;

import cz.nebixy.rainy.client.util.RainyBadge;

/**
 * Zapne/vypne ikonku Rainy (logo) před jmény lidí s Rainy Clientem.
 */
public class RainyBadgeSetting extends Module {
	public RainyBadgeSetting() {
		super("rainy_badge", "Ikonka Rainy", "Logo před jménem lidí s Rainy Clientem (TAB i nad hlavou).", Category.VISUAL, true);
	}

	@Override
	protected void onEnable() {
		RainyBadge.enabled = true;
	}

	@Override
	protected void onDisable() {
		RainyBadge.enabled = false;
	}

	@Override
	public void afterLoad() {
		RainyBadge.enabled = isEnabled();
	}
}
