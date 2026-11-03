package cz.nebixy.rainy.client.module;

/**
 * Srdíčka: nad hráči se ukáže ❤ a kolik mají životů. Samotné kreslení dělá HealthTagMixin.
 */
public class HealthTagsModule extends Module {
	public static volatile boolean active;

	public HealthTagsModule() {
		super("health_tags", "Srdíčka", "Nad hráči ukáže ❤ a kolik mají životů.", Category.PVP, true);
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
