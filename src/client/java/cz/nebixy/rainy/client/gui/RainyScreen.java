package cz.nebixy.rainy.client.gui;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import cz.nebixy.rainy.RainyClient;
import cz.nebixy.rainy.client.RainyConfig;
import cz.nebixy.rainy.client.module.AccentColorSetting;
import cz.nebixy.rainy.client.module.Category;
import cz.nebixy.rainy.client.module.KeySetting;
import cz.nebixy.rainy.client.module.Module;
import cz.nebixy.rainy.client.module.ModuleManager;
import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.Keys;
import cz.nebixy.rainy.client.util.Theme;

/**
 * Hlavní menu Rainy Clientu (Pravý Shift nebo ESC → Rainy Settings).
 * Vlevo kategorie, vpravo seznam modulů. Kliknutím na řádek modul zapneš/vypneš,
 * tlačítkem vpravo mu nastavíš klávesu.
 */
public class RainyScreen extends Screen {
	private static final int SIDEBAR_W = 100;
	private static final int ROW_H = 32;
	private static final int GAP = 4;
	private static final int HEADER_H = 36;
	private static final int FOOTER_H = 24;

	private static Category selected = Category.HUD;

	private final Screen parent;
	private final RainEffect rain = new RainEffect();
	private final long openedAt = System.currentTimeMillis();

	private float scroll;
	private float scrollTarget;
	private int maxScroll;

	/** Modul, kterému se právě nastavuje klávesa (null = nic). */
	private Module binding;

	// Rozložení se počítá při každém vykreslení
	private int px, py, pw, ph;

	/**
	 * @param parent obrazovka, kam se vrátit po zavření (ESC menu), nebo null = rovnou do hry
	 */
	public RainyScreen(Screen parent) {
		super(Component.literal("Rainy Client"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		// Z ESC menu zůstane hra pauznutá, z klávesy běží dál
		return parent != null;
	}

	@Override
	public void onClose() {
		RainyConfig.save();
		this.minecraft.gui.setScreen(parent);
	}

	private void layout() {
		pw = Math.min(440, this.width - 16);
		ph = Math.min(260, this.height - 16);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
	}

	private int listX() {
		return px + SIDEBAR_W + 12;
	}

	private int listW() {
		return pw - SIDEBAR_W - 24;
	}

	private int listTop() {
		return py + HEADER_H;
	}

	private int listBottom() {
		return py + ph - FOOTER_H;
	}

	// ---------------------------------------------------------------- vykreslení

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Bez rozmazání, jen ztmavení, ať je vidět hra
		g.fill(0, 0, this.width, this.height, Theme.OVERLAY);
		rain.render(g, this.width, this.height);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		layout();

		// Jemné vyjetí po otevření (posun o celé pixely, ať je text ostrý)
		float open = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
		float ease = 1f - (1f - open) * (1f - open);
		int slide = Math.round((1f - ease) * 10);

		g.pose().pushMatrix();
		g.pose().translate(0, slide);

		Draw.rect(g, px, py, px + pw, py + ph, Theme.PANEL, 3);
		Draw.rect(g, px, py, px + SIDEBAR_W, py + ph, Theme.SIDEBAR, 3);
		g.fill(px + SIDEBAR_W - 1, py + 4, px + SIDEBAR_W, py + ph - 4, Theme.BORDER);

		renderSidebar(g, mouseX, mouseY - slide);
		renderList(g, mouseX, mouseY - slide);

		g.pose().popMatrix();
	}

	private void renderSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		// Logo: dvojnásobná velikost (celé číslo = ostré pixely)
		g.pose().pushMatrix();
		g.pose().translate(px + 12, py + 12);
		g.pose().scale(2f);
		Draw.flat(g, "Rainy", 0, 0, Theme.accent());
		g.pose().popMatrix();
		Draw.flat(g, "CLIENT", px + 13, py + 32, Theme.TEXT_DIM);

		int y = py + 52;
		for (Category c : Category.values()) {
			boolean active = c == selected;
			boolean hover = Draw.inside(mouseX, mouseY, px + 6, y, px + SIDEBAR_W - 6, y + 20);
			if (active || hover) {
				Draw.rect(g, px + 6, y, px + SIDEBAR_W - 6, y + 20, active ? Theme.CARD_HOVER : Theme.CARD, 2);
			}
			if (active) {
				g.fill(px + 6, y + 4, px + 8, y + 16, Theme.accent());
			}
			Draw.flat(g, c.title, px + 14, y + 6, active ? Theme.TEXT : Theme.TEXT_DIM);
			// počet modulů v kategorii, vpravo
			String count = String.valueOf(ModuleManager.byCategory(c).size());
			Draw.flat(g, count, px + SIDEBAR_W - 12 - Draw.width(count), y + 6, active ? Theme.accent() : 0xFF6B7789);
			y += 23;
		}

		int bx1 = px + 8;
		int bx2 = px + SIDEBAR_W - 8;
		int by2 = py + ph - 8;
		int by1 = by2 - 20;
		boolean hover = Draw.inside(mouseX, mouseY, bx1, by1, bx2, by2);
		Draw.rect(g, bx1, by1, bx2, by2, hover ? Theme.TEXT : Theme.accent(), 2);
		Draw.flatCentered(g, "Upravit HUD", (bx1 + bx2) / 2, by1 + 6, Theme.TEXT_DARK);
	}

	private void renderList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = listX();
		int w = listW();

		// Hlavička
		Draw.flat(g, selected.title, x, py + 14, Theme.TEXT);
		Draw.flat(g, selected.subtitle, x + Draw.width(selected.title) + 8, py + 14, Theme.TEXT_DIM);
		String version = "v" + RainyClient.VERSION;
		Draw.flat(g, version, px + pw - 12 - Draw.width(version), py + 14, Theme.TEXT_DIM);

		List<Module> modules = ModuleManager.byCategory(selected);
		int top = listTop();
		int bottom = listBottom();
		int contentH = modules.size() * (ROW_H + GAP) - GAP;
		maxScroll = Math.max(0, contentH - (bottom - top));
		scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
		scroll += (scrollTarget - scroll) * 0.35f;
		int scrollPx = Math.round(scroll);

		boolean mouseInList = Draw.inside(mouseX, mouseY, x, top, x + w, bottom);
		g.enableScissor(x - 2, top, x + w + 2, bottom);
		for (int i = 0; i < modules.size(); i++) {
			int y1 = top + i * (ROW_H + GAP) - scrollPx;
			if (y1 > bottom || y1 + ROW_H < top) {
				continue;
			}
			renderRow(g, modules.get(i), x, y1, x + w, y1 + ROW_H, mouseInList, mouseX, mouseY);
		}
		g.disableScissor();

		if (maxScroll > 0) {
			int trackH = bottom - top;
			int barH = Math.max(16, trackH * trackH / contentH);
			int barY = top + Math.round((trackH - barH) * (scroll / maxScroll));
			g.fill(px + pw - 6, barY, px + pw - 4, barY + barH, Theme.accent(140));
		}

		// Patička: nápověda, nebo co dělat při nastavování klávesy
		String footer = binding != null
				? "Zmáčkni klávesu pro " + binding.name + "   ESC = zrušit   Backspace = smazat"
				: "Klik = zapnout/vypnout   Bind = klávesa pro modul";
		Draw.flat(g, Draw.trim(footer, w), x, py + ph - 16, binding != null ? Theme.accent() : Theme.TEXT_DIM);
	}

	private void renderRow(GuiGraphicsExtractor g, Module m, int x1, int y1, int x2, int y2, boolean mouseInList, int mouseX, int mouseY) {
		boolean hover = mouseInList && Draw.inside(mouseX, mouseY, x1, y1, x2, y2);
		Draw.rect(g, x1, y1, x2, y2, hover ? Theme.CARD_HOVER : Theme.CARD, 3);

		float target = m.isEnabled() ? 1f : 0f;
		m.toggleAnim += (target - m.toggleAnim) * 0.3f;
		boolean showsToggle = !(m instanceof KeySetting) && m.valueText() == null;
		if (showsToggle && m.isEnabled()) {
			g.fill(x1, y1 + 6, x1 + 2, y2 - 6, Theme.accent((int) (255 * m.toggleAnim)));
		}

		// Ovládání vpravo (první řádek): přepínač / hodnota, a tlačítko klávesy
		int right = x2 - 8;
		if (m instanceof AccentColorSetting) {
			right = drawSwatches(g, right, y1 + 6, mouseX, mouseY);
		} else if (m instanceof KeySetting ks) {
			right = drawChip(g, ks.keyName(), right, y1 + 5, m == binding, mouseX, mouseY, true);
		} else {
			String value = m.valueText();
			if (value == null) {
				Draw.toggle(g, right - 18, y1 + 7, m.toggleAnim);
				right -= 18 + 8;
			} else {
				int vw = Draw.width(value);
				Draw.flat(g, value, right - vw, y1 + 8, Theme.accent());
				right -= vw;
				if (m.id.equals("accent")) {
					Draw.rect(g, right - 12, y1 + 8, right - 4, y1 + 16, Theme.accent(), 2);
					right -= 12;
				}
				right -= 8;
			}
			if (m.bindable()) {
				String label = m.bindKey >= 0 ? Keys.name(m.bindKey) : "Bind";
				right = drawChip(g, label, right, y1 + 5, m == binding, mouseX, mouseY, m.bindKey >= 0);
			}
		}

		// Název (1. řádek) a popis (2. řádek, přes celou šířku)
		Draw.flat(g, Draw.trim(m.name, right - x1 - 18), x1 + 10, y1 + 8, Theme.TEXT);
		Draw.flat(g, Draw.trim(m.descriptionText(), x2 - x1 - 18), x1 + 10, y1 + 20, Theme.TEXT_DIM);
	}

	private static final int SWATCH = 10;
	private static final int SWATCH_GAP = 3;

	/** Levý okraj prvního čtverečku barvy (kreslí se zprava). */
	private static int swatchesLeft(int right) {
		int n = Theme.ACCENTS.length;
		return right - n * SWATCH - (n - 1) * SWATCH_GAP;
	}

	/** Řada barevných čtverečků. Vybraný má bílý rámeček, poslední je duha. */
	private int drawSwatches(GuiGraphicsExtractor g, int right, int y, int mouseX, int mouseY) {
		int x = swatchesLeft(right);
		int selectedIndex = Math.floorMod(Theme.accentIndex, Theme.ACCENTS.length);
		for (int i = 0; i < Theme.ACCENTS.length; i++) {
			int sx = x + i * (SWATCH + SWATCH_GAP);
			boolean hover = Draw.inside(mouseX, mouseY, sx - 1, y - 1, sx + SWATCH + 1, y + SWATCH + 1);
			if (i == selectedIndex || hover) {
				Draw.rect(g, sx - 2, y - 2, sx + SWATCH + 2, y + SWATCH + 2, i == selectedIndex ? Theme.TEXT : 0x80FFFFFF, 2);
			}
			if (i == Theme.RAINBOW) {
				// duha: pět svislých pruhů různých barev
				for (int b = 0; b < 5; b++) {
					g.fill(sx + b * 2, y, sx + b * 2 + 2, y + SWATCH, Theme.rainbow(b * 0.2f));
				}
			} else {
				Draw.rect(g, sx, y, sx + SWATCH, y + SWATCH, Theme.ACCENTS[i], 2);
			}
		}
		return x - 8;
	}

	/** Na který čtvereček barvy se kliklo (-1 = žádný). */
	private static int swatchAt(double mx, double my, int right, int y) {
		int x = swatchesLeft(right);
		for (int i = 0; i < Theme.ACCENTS.length; i++) {
			int sx = x + i * (SWATCH + SWATCH_GAP);
			if (Draw.inside(mx, my, sx - 1, y - 1, sx + SWATCH + 2, y + SWATCH + 2)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Tlačítko s klávesou. Kreslí se zprava doleva, vrací novou pravou hranu.
	 */
	private int drawChip(GuiGraphicsExtractor g, String label, int right, int y, boolean listening, int mouseX, int mouseY, boolean bound) {
		String text = listening ? "..." : label;
		int w = Math.max(28, Draw.width(text) + 10);
		int x1 = right - w;
		boolean hover = Draw.inside(mouseX, mouseY, x1, y, right, y + 12);
		int bg = listening ? Theme.accent() : hover ? 0xFF2C384C : 0xFF222C3D;
		Draw.rect(g, x1, y, right, y + 12, bg, 2);
		int color = listening ? Theme.TEXT_DARK : bound ? Theme.TEXT : Theme.TEXT_DIM;
		Draw.flatCentered(g, text, x1 + w / 2, y + 2, color);
		return x1 - 6;
	}

	/** Spočítá, kde leží tlačítko klávesy u daného řádku (stejně jako při kreslení). */
	private int[] chipBounds(Module m, int x1, int y1, int x2) {
		int right = x2 - 8;
		String label;
		if (m instanceof KeySetting ks) {
			label = ks.keyName();
		} else {
			if (!m.bindable()) {
				return null;
			}
			String value = m.valueText();
			if (value == null) {
				right -= 18 + 8;
			} else {
				right -= Draw.width(value) + (m.id.equals("accent") ? 12 : 0) + 8;
			}
			label = m.bindKey >= 0 ? Keys.name(m.bindKey) : "Bind";
		}
		String text = m == binding ? "..." : label;
		int w = Math.max(28, Draw.width(text) + 10);
		return new int[]{right - w, y1 + 5, right, y1 + 17};
	}

	// ---------------------------------------------------------------- vstup

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (binding == null) {
			return super.keyPressed(event);
		}
		int key = event.key();
		int newKey;
		if (key == InputConstants.KEY_ESCAPE) {
			binding = null; // zrušit, klávesa zůstane stejná
			return true;
		} else if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
			newKey = -1; // smazat klávesu
		} else {
			newKey = key;
		}
		if (binding instanceof KeySetting ks) {
			ks.setKey(newKey);
		} else {
			binding.bindKey = newKey;
			binding.bindWasDown = true; // ať se modul hned nepřepne, když zavřeš menu se stále drženou klávesou
		}
		binding = null;
		RainyConfig.save();
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
		layout();
		double mx = click.x();
		double my = click.y();

		// Kliknutí mimo při nastavování klávesy = zrušit
		if (binding != null) {
			binding = null;
		}

		int y = py + 52;
		for (Category c : Category.values()) {
			if (Draw.inside(mx, my, px + 6, y, px + SIDEBAR_W - 6, y + 20)) {
				if (selected != c) {
					selected = c;
					scroll = 0;
					scrollTarget = 0;
				}
				return true;
			}
			y += 23;
		}

		int by2 = py + ph - 8;
		if (Draw.inside(mx, my, px + 8, by2 - 20, px + SIDEBAR_W - 8, by2)) {
			RainyConfig.save();
			this.minecraft.gui.setScreen(new HudEditorScreen(this));
			return true;
		}

		int x = listX();
		int w = listW();
		if (Draw.inside(mx, my, x, listTop(), x + w, listBottom())) {
			List<Module> modules = ModuleManager.byCategory(selected);
			int scrollPx = Math.round(scroll);
			for (int i = 0; i < modules.size(); i++) {
				Module m = modules.get(i);
				int y1 = listTop() + i * (ROW_H + GAP) - scrollPx;
				if (!Draw.inside(mx, my, x, y1, x + w, y1 + ROW_H)) {
					continue;
				}
				if (m instanceof AccentColorSetting accent) {
					int swatch = swatchAt(mx, my, x + w - 8, y1 + 6);
					if (swatch >= 0) {
						accent.select(swatch);
						RainyConfig.save();
					}
					return true;
				}
				int[] chip = chipBounds(m, x, y1, x + w);
				if (m instanceof KeySetting || chip != null && Draw.inside(mx, my, chip[0], chip[1], chip[2], chip[3])) {
					binding = m; // čekáme na klávesu
				} else {
					m.onClick();
					RainyConfig.save();
				}
				return true;
			}
		}

		return super.mouseClicked(click, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scrollTarget -= (float) verticalAmount * 20f;
		scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
		return true;
	}
}
