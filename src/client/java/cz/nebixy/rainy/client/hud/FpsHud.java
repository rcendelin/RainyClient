package cz.nebixy.rainy.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.client.util.InputTracker;

public class FpsHud extends HudModule {
	public FpsHud() {
		super("fps", "FPS", "Ukazuje snímky za sekundu.", true, 4, 4);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		return drawLabel(g, x, y, "FPS", String.valueOf(InputTracker.fps()));
	}
}
