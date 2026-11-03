package cz.nebixy.rainy.client.module;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Potato Graphics: co nejvíc FPS, i za cenu horšího vzhledu.
 * Všechno z FPS Boostu a navíc menší dohled, bez mipmap, bez plynulého načítání chunků a bez rozmazání menu.
 */
public class PotatoGraphicsModule extends SettingsPresetModule {
	private static final Map<String, String> PRESET = new LinkedHashMap<>();

	static {
		PRESET.put("entityShadows", "false");
		PRESET.put("particles", "ENUM:MINIMAL");
		PRESET.put("cloudStatus", "ENUM:OFF");
		PRESET.put("ambientOcclusion", "false");
		PRESET.put("biomeBlendRadius", "0");
		PRESET.put("entityDistanceScaling", "0.5");   // nejmenší možné
		PRESET.put("enableVsync", "false");
		PRESET.put("framerateLimit", "260");          // Unlimited
		PRESET.put("improvedTransparency", "false");
		PRESET.put("cutoutLeaves", "false");
		PRESET.put("renderDistance", "MAX:6");        // dohled max 6 chunků (jen snížit, nikdy nezvýšit)
		PRESET.put("simulationDistance", "MAX:5");
		PRESET.put("mipmapLevels", "0");
		PRESET.put("chunkSectionFadeInTime", "0.0");  // chunky naskočí hned
		PRESET.put("menuBackgroundBlurriness", "0");
		PRESET.put("vignette", "false");
	}

	public PotatoGraphicsModule() {
		super("potato_graphics", "Potato Graphics", "Nejvíc FPS: všechno z FPS Boostu, menší dohled a jednodušší grafika.", PRESET);
	}
}
