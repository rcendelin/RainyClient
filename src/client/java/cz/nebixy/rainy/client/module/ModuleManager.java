package cz.nebixy.rainy.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;

import cz.nebixy.rainy.client.RainyConfig;
import cz.nebixy.rainy.client.RainyKeys;
import cz.nebixy.rainy.client.hud.Notification;
import cz.nebixy.rainy.client.util.Keys;

import cz.nebixy.rainy.RainyClient;
import cz.nebixy.rainy.client.hud.ArmorHud;
import cz.nebixy.rainy.client.hud.ClockHud;
import cz.nebixy.rainy.client.hud.CoordsHud;
import cz.nebixy.rainy.client.hud.CpsHud;
import cz.nebixy.rainy.client.hud.FpsHud;
import cz.nebixy.rainy.client.hud.HudModule;
import cz.nebixy.rainy.client.hud.KeystrokesHud;
import cz.nebixy.rainy.client.hud.PingHud;

/**
 * Seznam všech modulů. Nový modul přidáš do metody init().
 */
public final class ModuleManager {
	private ModuleManager() {
	}

	private static final List<Module> MODULES = new ArrayList<>();
	private static final List<HudModule> HUD = new ArrayList<>();

	public static ZoomModule zoom;
	public static FullbrightModule fullbright;

	public static void init() {
		// HUD: info na obrazovce
		add(new FpsHud());
		add(new PingHud());
		add(new CpsHud());
		add(new KeystrokesHud());
		add(new ArmorHud());
		add(new PotionTimerModule());
		add(new CoordsHud());
		add(new ClockHud());

		// PvP: věci do boje
		add(new ToggleSprintModule());
		zoom = add(new ZoomModule());
		add(new HealthTagsModule());

		// Vizuál: jak hra vypadá
		fullbright = add(new FullbrightModule());
		add(new ClearGlassModule());
		add(new ShulkerPreviewModule());
		add(new RainyBadgeSetting());

		// FPS: výkon
		add(new FpsBoostModule());
		add(new PotatoGraphicsModule());
		add(new FpsTestModule());

		// Nastavení: client samotný
		add(new AccentColorSetting());
		add(new HudBackgroundSetting());
		add(new ResetHudAction());
		add(new KeySetting("key_menu", "Klávesa menu", "Otevře tohle menu.", RainyKeys.MENU));
		add(new KeySetting("key_zoom", "Klávesa zoomu", "Drž ji a přiblížíš si obraz.", RainyKeys.ZOOM));
		add(new KeySetting("key_hud", "Klávesa HUD editoru", "Otevře přesouvání HUD modulů.", RainyKeys.HUD_EDITOR));

		RainyClient.LOGGER.info("[Rainy] Načteno {} modulů", MODULES.size());
	}

	private static <T extends Module> T add(T module) {
		MODULES.add(module);
		if (module instanceof HudModule hud) {
			HUD.add(hud);
		}
		return module;
	}

	public static List<Module> all() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<HudModule> hud() {
		return Collections.unmodifiableList(HUD);
	}

	public static List<Module> byCategory(Category category) {
		List<Module> list = new ArrayList<>();
		for (Module m : MODULES) {
			if (m.category == category) {
				list.add(m);
			}
		}
		return list;
	}

	public static void tick(Minecraft mc) {
		handleBinds(mc);
		for (Module m : MODULES) {
			if (m.isEnabled()) {
				m.onTick(mc);
			}
		}
	}

	/** Zapne/vypne moduly, které mají nastavenou klávesu. Jen ve hře, ne v chatu ani v menu. */
	private static void handleBinds(Minecraft mc) {
		boolean inGame = mc.player != null && mc.gui.screen() == null;
		boolean changed = false;
		for (Module m : MODULES) {
			if (m.bindKey < 0 || !m.bindable()) {
				m.bindWasDown = false;
				continue;
			}
			boolean down = inGame && Keys.isDown(m.bindKey);
			if (down && !m.bindWasDown) {
				m.toggle();
				Notification.show(m.name, m.isEnabled());
				changed = true;
			}
			m.bindWasDown = down;
		}
		if (changed) {
			RainyConfig.save();
		}
	}
}
