package cz.nebixy.rainy.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.client.util.Compat;
import cz.nebixy.rainy.client.util.PingTracker;

public class PingHud extends HudModule {
	public PingHud() {
		super("ping", "Ping", "Skutečná odezva na server v ms (měří se sama).", true, 4, 42);
	}

	@Override
	public void onTick(Minecraft mc) {
		PingTracker.tick(mc);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		int ping = PingTracker.ping();
		if (ping < 0) {
			ping = Compat.ping(); // server na ping neodpovídá: aspoň číslo z TABu
		}
		String value = ping < 0 ? "-" : ping + " ms";
		return drawLabel(g, x, y, "PING", value);
	}
}
