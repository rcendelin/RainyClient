package cz.nebixy.rainy.client.module;

/**
 * Shulker Preview: najeď myší na shulker a uvidíš, co v něm je (mřížka v barvě shulkeru).
 * Kreslení dělá ShulkerTooltip, napojení na tooltip ShulkerTooltipMixin.
 */
public class ShulkerPreviewModule extends Module {
	public static volatile boolean active;

	public ShulkerPreviewModule() {
		super("shulker_preview", "Shulker Preview", "Najeď myší na shulker a uvidíš, co v něm je.", Category.VISUAL, true);
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
