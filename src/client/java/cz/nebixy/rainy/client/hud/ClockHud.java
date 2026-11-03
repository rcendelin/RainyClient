package cz.nebixy.rainy.client.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ClockHud extends HudModule {
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	public ClockHud() {
		super("clock", "Hodiny", "Reálný čas, ať víš, kdy jít spát.", false, 72, 4);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		return drawLabel(g, x, y, "ČAS", LocalTime.now().format(FORMAT));
	}
}
