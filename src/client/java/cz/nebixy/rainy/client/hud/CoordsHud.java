package cz.nebixy.rainy.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

public class CoordsHud extends HudModule {
	private static final String[] DIRECTIONS = {"Jih (+Z)", "Západ (-X)", "Sever (-Z)", "Východ (+X)"};

	public CoordsHud() {
		super("coords", "Souřadnice", "XYZ a směr, kterým se díváš.", true, 4, 61);
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		LocalPlayer player = Minecraft.getInstance().player;
		double px = player == null ? 128.5 : player.getX();
		double py = player == null ? 64 : player.getY();
		double pz = player == null ? -42.5 : player.getZ();
		float yaw = player == null ? 0 : player.getYRot();
		int dir = Math.floorMod(Math.round(yaw / 90f), 4);

		String[][] lines = {
				{"X", String.format("%.1f", px)},
				{"Y", String.format("%.0f", Math.floor(py))},
				{"Z", String.format("%.1f", pz)},
				{"SMĚR", DIRECTIONS[dir]}
		};
		return drawLines(g, x, y, lines);
	}
}
