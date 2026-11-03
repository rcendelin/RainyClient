package cz.nebixy.rainy.client.module;

public enum Category {
	HUD("HUD", "Info na obrazovce"),
	PVP("PvP", "Věci do boje"),
	VISUAL("Vizuál", "Jak hra vypadá"),
	FPS("FPS", "Výkon a plynulost"),
	SETTINGS("Nastavení", "Barva, HUD a klávesy");

	public final String title;
	public final String subtitle;

	Category(String title, String subtitle) {
		this.title = title;
		this.subtitle = subtitle;
	}
}
