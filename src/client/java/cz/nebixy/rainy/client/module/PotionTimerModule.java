package cz.nebixy.rainy.client.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.Theme;

/**
 * Potion Timer: ikony efektů vpravo nahoře jako ve vanille, ale pod každou je odpočet času.
 * Řádky jsou od sebe dál než ve vanille, aby se čas vešel pod ikonu.
 */
public class PotionTimerModule extends Module {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");
	private static final Identifier BACKGROUND_AMBIENT = Identifier.withDefaultNamespace("hud/effect_background_ambient");

	public static PotionTimerModule instance;

	public PotionTimerModule() {
		super("potion_timer", "Potion Timer", "Odpočet času pod ikonami efektů vpravo nahoře.", Category.HUD, true);
		instance = this;
	}

	/** Nahrazuje vanilla vykreslení efektů. Když je modul vypnutý, kreslí se původní. */
	public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker, HudElement original) {
		if (instance == null || !instance.isEnabled()) {
			original.extractRenderState(g, deltaTracker);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Collection<MobEffectInstance> active = mc.player.getActiveEffects();
		Screen screen = mc.gui.screen();
		if (active.isEmpty() || screen != null && screen.showsActiveEffects()) {
			return;
		}

		List<MobEffectInstance> effects = new ArrayList<>(active);
		effects.sort(Comparator.reverseOrder()); // stejné pořadí jako vanilla

		int good = 0;
		int bad = 0;
		for (MobEffectInstance effect : effects) {
			if (!effect.showIcon()) {
				continue;
			}
			int x;
			int y = 1;
			if (effect.getEffect().value().isBeneficial()) {
				good++;
				x = g.guiWidth() - 25 * good;
			} else {
				bad++;
				x = g.guiWidth() - 25 * bad;
				y += 36; // druhý řádek níž, ať se vejde čas
			}

			g.blitSprite(RenderPipelines.GUI_TEXTURED, effect.isAmbient() ? BACKGROUND_AMBIENT : BACKGROUND, x, y, 24, 24);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()), x + 3, y + 3, 18, 18);

			String time = formatTime(effect);
			int color = !effect.isInfiniteDuration() && effect.getDuration() <= 200 ? 0xFFFF5555 : Theme.TEXT;
			Draw.centeredText(g, time, x + 12, y + 26, color);
		}
	}

	/** 1:30, 12m, nebo ∞ pro nekonečné efekty. */
	private static String formatTime(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			return "∞";
		}
		int seconds = effect.getDuration() / 20;
		if (seconds >= 600) {
			return seconds / 60 + "m";
		}
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}
}
