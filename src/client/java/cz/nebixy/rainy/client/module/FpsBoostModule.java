package cz.nebixy.rainy.client.module;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FPS Boost: vypne věci, které žerou FPS, ale hra pořád vypadá normálně.
 */
public class FpsBoostModule extends SettingsPresetModule {
	private static final Map<String, String> PRESET = new LinkedHashMap<>();

	static {
		PRESET.put("entityShadows", "false");
		PRESET.put("particles", "ENUM:MINIMAL");
		PRESET.put("cloudStatus", "ENUM:OFF");
		PRESET.put("ambientOcclusion", "false");
		PRESET.put("biomeBlendRadius", "0");
		PRESET.put("entityDistanceScaling", "MAX:0.75");
		PRESET.put("enableVsync", "false");
		PRESET.put("framerateLimit", "260");          // Unlimited
		PRESET.put("improvedTransparency", "false");  // náročné shadery pro průhlednost
		PRESET.put("cutoutLeaves", "false");          // neprůhledné listí = hodně FPS v lese
	}

	public FpsBoostModule() {
		super("fps_boost", "FPS Boost", "Vypne stíny, mraky, částice a průhledné listí. Po vypnutí vrátí nastavení.", PRESET);
	}
}
