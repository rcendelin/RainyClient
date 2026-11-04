package cz.nebixy.rainy.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import cz.nebixy.rainy.client.util.Draw;

/**
 * Map Preview: najedeš myší na mapu a v tooltipu uvidíš, co je na ní (jako mod Map Tooltip).
 */
public class MapTooltip implements ClientTooltipComponent {
	private static final int MAP = 128;      // mapa má 128 x 128 pixelů
	private static final float SCALE = 0.75f; // v tooltipu 96 x 96
	private static final int SIZE = Math.round(MAP * SCALE);
	private static final int PAD = 3;

	/** Data pro tooltip: ID mapy. */
	public record Data(MapId id) implements TooltipComponent {
	}

	private final Data data;
	private final MapRenderState state = new MapRenderState();

	public MapTooltip(Data data) {
		this.data = data;
	}

	/** Vytvoří data z itemu, nebo null, když to není mapa nebo ji hra ještě nezná. */
	public static Data from(ItemStack stack) {
		MapId id = stack.get(DataComponents.MAP_ID);
		Minecraft mc = Minecraft.getInstance();
		if (id == null || mc.level == null || MapItem.getSavedData(id, mc.level) == null) {
			return null;
		}
		return new Data(id);
	}

	@Override
	public int getHeight(Font font) {
		return SIZE + PAD * 2 + 2;
	}

	@Override
	public int getWidth(Font font) {
		return SIZE + PAD * 2;
	}

	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		MapItemSavedData saved = MapItem.getSavedData(data.id(), mc.level);
		if (saved == null) {
			return;
		}
		// Rámeček jako papír mapy
		Draw.rect(g, x, y, x + SIZE + PAD * 2, y + SIZE + PAD * 2, 0xFF6B5636, 2);
		g.fill(x + 1, y + 1, x + SIZE + PAD * 2 - 1, y + SIZE + PAD * 2 - 1, 0xFFD9C9A3);

		mc.getMapRenderer().extractRenderState(data.id(), saved, state);
		g.pose().pushMatrix();
		g.pose().translate(x + PAD, y + PAD);
		g.pose().scale(SCALE, SCALE);
		g.map(state);
		g.pose().popMatrix();
	}
}
