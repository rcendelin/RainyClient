package cz.nebixy.rainy.client.module;

/**
 * Map Preview: náhled mapy v tooltipu. Kreslí ho MapTooltip, tady je jen vypínač.
 */
public class MapPreviewModule extends Module {
	/** Čte ho MapTooltipMixin. */
	public static volatile boolean active;

	public MapPreviewModule() {
		super("map_preview", "Map Preview", "Najeď myší na mapu a uvidíš, co je na ní.", Category.VISUAL, true);
	}

	@Override
	protected void onEnable() {
		active = true;
	}

	@Override
	protected void onDisable() {
		active = false;
	}

	@Override
	public void afterLoad() {
		active = isEnabled();
	}
}
