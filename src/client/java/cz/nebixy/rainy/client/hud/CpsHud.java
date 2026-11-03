package cz.nebixy.rainy.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.client.util.InputTracker;

public class CpsHud extends HudModule {
	public CpsHud() {
		super("cps", "CPS", "Kliky za sekundu (levé | pravé).", true, 4, 23);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		return drawLabel(g, x, y, "CPS", InputTracker.leftCps() + " | " + InputTracker.rightCps());
	}
}
