package cz.nebixy.rainy.client.module;

import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;

/**
 * Základ každého modulu. Modul jde zapnout a vypnout v menu (Pravý Shift).
 */
public abstract class Module {
	public final String id;
	public final String name;
	public final String description;
	public final Category category;

	private boolean enabled;
	private final boolean enabledByDefault;

	/** Animace přepínače v menu (0 = vypnuto, 1 = zapnuto). */
	public float toggleAnim;

	/** Klávesa, která modul zapne/vypne (-1 = žádná). Nastavuje se v menu tlačítkem Bind. */
	public int bindKey = -1;
	/** Pomocná proměnná, aby se modul přepnul jen jednou za zmáčknutí. */
	public boolean bindWasDown;

	protected Module(String id, String name, String description, Category category, boolean enabledByDefault) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.enabledByDefault = enabledByDefault;
		this.enabled = enabledByDefault;
		this.toggleAnim = enabledByDefault ? 1f : 0f;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	/** Co se stane po kliknutí na kartu v menu. Většina modulů se jen přepne. */
	public void onClick() {
		toggle();
	}

	/** Jde na modul nastavit klávesu? Nastavení (barva, reset...) ne. */
	public boolean bindable() {
		return category != Category.SETTINGS;
	}

	/** Popis pod názvem v menu (může se měnit, třeba u barvy). */
	public String descriptionText() {
		return description;
	}

	/** Text vpravo na kartě místo přepínače (null = ukázat přepínač). */
	public String valueText() {
		return null;
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Volá se 20x za sekundu, jen když je modul zapnutý. */
	public void onTick(Minecraft mc) {
	}

	// ---------------------------------------------------------------- config

	public void save(JsonObject json) {
		json.addProperty("enabled", enabled);
		if (bindKey >= 0) {
			json.addProperty("bind", bindKey);
		}
	}

	public void load(JsonObject json) {
		boolean value = json.has("enabled") ? json.get("enabled").getAsBoolean() : enabledByDefault;
		this.enabled = value;
		this.toggleAnim = value ? 1f : 0f;
		this.bindKey = json.has("bind") ? json.get("bind").getAsInt() : -1;
	}

	/** Spustí se po načtení configu, aby zapnuté moduly mohly nastavit hru. */
	public void afterLoad() {
		if (enabled) {
			onEnable();
		}
	}
}
