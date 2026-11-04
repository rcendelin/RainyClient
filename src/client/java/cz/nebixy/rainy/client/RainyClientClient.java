package cz.nebixy.rainy.client;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import cz.nebixy.rainy.RainyClient;
import cz.nebixy.rainy.client.gui.HudEditorScreen;
import cz.nebixy.rainy.client.gui.MapTooltip;
import cz.nebixy.rainy.client.gui.RainyScreen;
import cz.nebixy.rainy.client.gui.ShulkerTooltip;
import cz.nebixy.rainy.client.hud.HudModule;
import cz.nebixy.rainy.client.hud.Notification;
import cz.nebixy.rainy.client.module.FpsTestModule;
import cz.nebixy.rainy.client.module.ModuleManager;
import cz.nebixy.rainy.client.module.PotionTimerModule;
import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.InputTracker;
import cz.nebixy.rainy.client.util.Theme;

public class RainyClientClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		RainyKeys.init();
		ModuleManager.init();
		RainyConfig.load();

		// Klávesy a moduly, 20x za sekundu
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (RainyKeys.MENU.consumeClick()) {
				client.gui.setScreen(new RainyScreen(null));
			}
			while (RainyKeys.HUD_EDITOR.consumeClick()) {
				client.gui.setScreen(new HudEditorScreen(null));
			}
			ModuleManager.tick(client);
		});

		// HUD se kreslí každý snímek, před chatem (chat bude vždycky nad ním)
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(RainyClient.MOD_ID, "hud"), RainyClientClient::renderHud);

		// Ikony efektů vpravo nahoře s odpočtem času (Potion Timer)
		HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS,
				original -> (g, deltaTracker) -> PotionTimerModule.render(g, deltaTracker, original));

		// Shulker Preview a Map Preview: naše obrázky v tooltipu
		ClientTooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof ShulkerTooltip.Data d) {
				return new ShulkerTooltip(d);
			}
			if (data instanceof MapTooltip.Data d) {
				return new MapTooltip(d);
			}
			return null;
		});

		// Nápis na hlavní obrazovce + tlačítko Rainy Settings v ESC menu
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof TitleScreen) {
				ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> renderTitleBadge(g));
			}
			if (screen instanceof PauseScreen) {
				addPauseButton(client, screen);
			}
		});

		// Při vypnutí hry uložit config a vrátit FOV a jas, ať se do nastavení MC neuloží zoom nebo fullbright
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			if (ModuleManager.zoom != null) {
				ModuleManager.zoom.forceStop();
			}
			if (ModuleManager.fullbright != null) {
				ModuleManager.fullbright.restoreForShutdown();
			}
			RainyConfig.save();
		});

		RainyClient.LOGGER.info("[Rainy] {} {} je připravený. Menu = Pravý Shift", RainyClient.NAME, RainyClient.VERSION);
	}

	private static void renderHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		InputTracker.onFrame();
		FpsTestModule.onFrame();
		if (HudEditorScreen.isOpen()) {
			return; // editor si moduly kreslí sám
		}
		if (Minecraft.getInstance().player == null) {
			return;
		}
		for (HudModule module : ModuleManager.hud()) {
			if (module.isEnabled()) {
				module.render(g, false);
			}
		}
		Notification.render(g);
		FpsTestModule.render(g);
	}

	/**
	 * Přidá do ESC menu tlačítko "Rainy Settings" pod poslední tlačítko Minecraftu.
	 */
	private static void addPauseButton(Minecraft client, Screen screen) {
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		if (widgets.isEmpty()) {
			return; // ESC menu bez tlačítek (F3 + Esc)
		}

		int centerX = screen.width / 2;
		int bottom = -1;
		int width = 204;
		for (AbstractWidget w : widgets) {
			int wCenter = w.getX() + w.getWidth() / 2;
			if (Math.abs(wCenter - centerX) <= 110 && w.getY() + w.getHeight() > bottom) {
				bottom = w.getY() + w.getHeight();
				width = Math.max(98, Math.min(204, w.getWidth()));
			}
		}

		int x;
		int y;
		if (bottom > 0 && bottom + 4 + 20 <= screen.height - 4) {
			width = 204;
			x = centerX - width / 2;
			y = bottom + 4;
		} else {
			width = 98; // málo místa, dáme ho do levého horního rohu
			x = 4;
			y = 4;
		}

		widgets.add(Button.builder(Component.literal("Rainy Settings"),
				button -> client.gui.setScreen(new RainyScreen(screen)))
				.bounds(x, y, width, 20)
				.build());
	}

	private static void renderTitleBadge(GuiGraphicsExtractor g) {
		int x = 6;
		int y = 6;
		String name = "Rainy";
		String rest = " Client " + RainyClient.VERSION;
		int w = Draw.width(name + rest) + 12;
		Draw.rect(g, x, y, x + w, y + 16, 0xA0000000, 3);
		g.fill(x + 3, y + 15, x + w - 3, y + 16, Theme.accent());
		Draw.text(g, name, x + 6, y + 4, Theme.accent());
		Draw.text(g, rest, x + 6 + Draw.width(name), y + 4, Theme.TEXT);
	}
}
