package cz.nebixy.rainy.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import cz.nebixy.rainy.RainyClient;
import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.Theme;

/**
 * Stránka "Rainy" z ESC menu (jako Help v Lunaru). Zatím jen Under Construction.
 */
public class UnderConstructionScreen extends Screen {
	private final Screen parent;
	private final RainEffect rain = new RainEffect();

	public UnderConstructionScreen(Screen parent) {
		super(Component.literal("Rainy"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(Component.literal("Zpět"), b -> onClose())
				.bounds(this.width / 2 - 50, this.height / 2 + 52, 100, 20)
				.build());
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(parent);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, 0xF0060A12);
		rain.render(g, this.width, this.height);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int cy = this.height / 2;

		// Velké logo "Rainy" (4x, celé číslo = ostré pixely)
		g.pose().pushMatrix();
		g.pose().translate(cx - Draw.width("Rainy") * 2, cy - 62);
		g.pose().scale(4f);
		Draw.flat(g, "Rainy", 0, 0, Theme.accent());
		g.pose().popMatrix();
		Draw.flatCentered(g, "CLIENT " + RainyClient.VERSION, cx, cy - 24, Theme.TEXT_DIM);

		// Pruh "UNDER CONSTRUCTION" jako výstražná páska
		int w = Draw.width("UNDER CONSTRUCTION") * 2 + 40;
		int x1 = cx - w / 2;
		int y1 = cy - 4;
		Draw.rect(g, x1, y1, x1 + w, y1 + 30, Theme.accent(), 3);
		for (int sx = x1 + 4; sx < x1 + w - 4; sx += 12) {
			g.fill(sx, y1 + 2, sx + 6, y1 + 4, 0xFF0B1220);
			g.fill(sx + 6, y1 + 26, sx + 12 > x1 + w - 4 ? x1 + w - 4 : sx + 12, y1 + 28, 0xFF0B1220);
		}
		g.pose().pushMatrix();
		g.pose().translate(cx - Draw.width("UNDER CONSTRUCTION"), y1 + 8);
		g.pose().scale(2f);
		Draw.flat(g, "UNDER CONSTRUCTION", 0, 0, Theme.TEXT_DARK);
		g.pose().popMatrix();

		Draw.flatCentered(g, "Na téhle stránce se pracuje. Brzy tu bude víc.", cx, cy + 36, Theme.TEXT);
	}
}
