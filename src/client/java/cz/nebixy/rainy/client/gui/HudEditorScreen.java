package cz.nebixy.rainy.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import cz.nebixy.rainy.client.RainyConfig;
import cz.nebixy.rainy.client.hud.HudModule;
import cz.nebixy.rainy.client.module.ModuleManager;
import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.Theme;

/**
 * HUD editor: chytni modul myší a přetáhni ho, kam chceš.
 * U okrajů a středu obrazovky se modul sám přichytí.
 */
public class HudEditorScreen extends Screen {
	private static final int SNAP = 4;
	private static long lastFrame;

	private final Screen parent;
	private HudModule dragging;
	private double offsetX;
	private double offsetY;
	private boolean snappedCenterX;
	private boolean snappedCenterY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	/** True, když je editor právě na obrazovce (normální HUD se pak nekreslí dvakrát). */
	public static boolean isOpen() {
		return System.currentTimeMillis() - lastFrame < 150;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		RainyConfig.save();
		lastFrame = 0;
		this.minecraft.gui.setScreen(parent);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, 0x50000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		lastFrame = System.currentTimeMillis();

		// Vodítka středu při přichycení
		if (dragging != null && snappedCenterX) {
			g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, Theme.accent(120));
		}
		if (dragging != null && snappedCenterY) {
			g.fill(0, this.height / 2, this.width, this.height / 2 + 1, Theme.accent(120));
		}

		HudModule hovered = dragging != null ? dragging : moduleAt(mouseX, mouseY);
		for (HudModule m : ModuleManager.hud()) {
			if (!m.isEnabled()) {
				continue;
			}
			m.render(g, true);
			int color = m == hovered ? Theme.accent() : 0x70FFFFFF;
			Draw.dashedBorder(g, m.lastX - 2, m.lastY - 2, m.lastX + m.width + 2, m.lastY + m.height + 2, color);
		}

		// Štítek nad modulem pod myší: název a velikost
		if (hovered != null) {
			String label = hovered.name + "  " + Math.round(hovered.scale * 100) + " %";
			int lw = Draw.width(label) + 8;
			int lx = Math.max(0, Math.min(hovered.lastX, this.width - lw));
			int ly = hovered.lastY - 16;
			if (ly < 0) {
				// Nahoře není místo: dát štítek vpravo vedle modulu, jinak pod něj
				if (hovered.lastX + hovered.width + 6 + lw <= this.width) {
					lx = hovered.lastX + hovered.width + 6;
					ly = hovered.lastY;
				} else {
					ly = hovered.lastY + hovered.height + 4;
				}
			}
			Draw.rect(g, lx, ly, lx + lw, ly + 13, Theme.accent(), 2);
			Draw.flat(g, label, lx + 4, ly + 3, Theme.TEXT_DARK);
		}

		// Nápověda
		String hint = "Táhni = posun   Kolečko = velikost   ESC = hotovo";
		int w = Draw.width(hint) + 16;
		int x = (this.width - w) / 2;
		int y = this.height - 40;
		Draw.rect(g, x, y, x + w, y + 18, 0xC0000000, 3);
		Draw.flatCentered(g, hint, this.width / 2, y + 5, Theme.TEXT);
	}

	private HudModule moduleAt(double mx, double my) {
		List<HudModule> list = ModuleManager.hud();
		for (int i = list.size() - 1; i >= 0; i--) {
			HudModule m = list.get(i);
			if (m.isEnabled() && Draw.inside(mx, my, m.lastX - 2, m.lastY - 2, m.lastX + m.width + 2, m.lastY + m.height + 2)) {
				return m;
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
		HudModule m = moduleAt(click.x(), click.y());
		if (m != null) {
			dragging = m;
			m.detach();
			m.x = m.lastX;
			m.y = m.lastY;
			offsetX = click.x() - m.x;
			offsetY = click.y() - m.y;
			return true;
		}
		return super.mouseClicked(click, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) {
			return super.mouseDragged(event, dragX, dragY);
		}
		int w = dragging.width;
		int h = dragging.height;
		int nx = (int) Math.round(event.x() - offsetX);
		int ny = (int) Math.round(event.y() - offsetY);

		// Přichycení k okrajům
		if (Math.abs(nx) < SNAP) nx = 0;
		if (Math.abs(ny) < SNAP) ny = 0;
		if (Math.abs(nx + w - this.width) < SNAP) nx = this.width - w;
		if (Math.abs(ny + h - this.height) < SNAP) ny = this.height - h;

		// Přichycení ke středu
		snappedCenterX = Math.abs(nx + w / 2 - this.width / 2) < SNAP;
		snappedCenterY = Math.abs(ny + h / 2 - this.height / 2) < SNAP;
		if (snappedCenterX) nx = this.width / 2 - w / 2;
		if (snappedCenterY) ny = this.height / 2 - h / 2;

		dragging.x = Math.max(0, Math.min(nx, this.width - w));
		dragging.y = Math.max(0, Math.min(ny, this.height - h));
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		HudModule m = dragging != null ? dragging : moduleAt(mouseX, mouseY);
		if (m == null || verticalAmount == 0) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}
		m.x = m.lastX;
		m.y = m.lastY;
		m.changeScale(verticalAmount > 0 ? 1 : -1);
		RainyConfig.save();
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			snappedCenterX = false;
			snappedCenterY = false;
			RainyConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}
}
