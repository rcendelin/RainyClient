package cz.nebixy.rainy.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import cz.nebixy.rainy.client.util.Draw;

/**
 * Náhled obsahu shulkeru v tooltipu: 9 x 3 sloty v barvě shulkeru, i s počty itemů.
 */
public class ShulkerTooltip implements ClientTooltipComponent {
	private static final int COLUMNS = 9;
	private static final int ROWS = 3;
	private static final int SLOT = 18;
	private static final int PAD = 4;
	private static final int DEFAULT_COLOR = 0x8B5E9C; // nebarvený shulker

	/** Data pro tooltip: obsah shulkeru (27 slotů) a jeho barva. */
	public record Data(NonNullList<ItemStack> items, int color) implements TooltipComponent {
	}

	private final Data data;

	public ShulkerTooltip(Data data) {
		this.data = data;
	}

	/** Je to shulker? */
	public static boolean isShulker(ItemStack stack) {
		return stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock;
	}

	/** Vytvoří data z itemu, nebo null, když to není shulker nebo je prázdný. */
	public static Data from(ItemStack stack) {
		if (!isShulker(stack)) {
			return null;
		}
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null) {
			return null;
		}
		NonNullList<ItemStack> items = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
		contents.copyInto(items);
		boolean any = false;
		for (ItemStack s : items) {
			any |= !s.isEmpty();
		}
		if (!any) {
			return null;
		}
		DyeColor dye = ((ShulkerBoxBlock) ((BlockItem) stack.getItem()).getBlock()).getColor();
		int color = dye == null ? DEFAULT_COLOR : dye.getTextureDiffuseColor() & 0xFFFFFF;
		return new Data(items, color);
	}

	@Override
	public int getHeight(Font font) {
		return ROWS * SLOT + PAD * 2 + 2;
	}

	@Override
	public int getWidth(Font font) {
		return COLUMNS * SLOT + PAD * 2;
	}

	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor g) {
		int w = COLUMNS * SLOT + PAD * 2;
		int h = ROWS * SLOT + PAD * 2;
		int base = data.color();

		// Panel v barvě shulkeru: světlejší okraj, tmavší vnitřek, ještě tmavší sloty
		Draw.rect(g, x, y, x + w, y + h, 0xFF000000 | shade(base, 1.15f), 3);
		Draw.rect(g, x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000 | shade(base, 0.55f), 2);

		for (int i = 0; i < data.items().size(); i++) {
			int sx = x + PAD + (i % COLUMNS) * SLOT;
			int sy = y + PAD + (i / COLUMNS) * SLOT;
			g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0xFF000000 | shade(base, 0.35f));
			g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0xFF000000 | shade(base, 0.42f));
			ItemStack stack = data.items().get(i);
			if (!stack.isEmpty()) {
				g.item(stack, sx + 1, sy + 1);
				g.itemDecorations(font, stack, sx + 1, sy + 1); // počet a výdrž
			}
		}
	}

	/** Zesvětlí/ztmaví RGB barvu (factor < 1 = tmavší). */
	private static int shade(int rgb, float factor) {
		int r = Math.min(255, Math.round((rgb >> 16 & 0xFF) * factor));
		int gr = Math.min(255, Math.round((rgb >> 8 & 0xFF) * factor));
		int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
		return r << 16 | gr << 8 | b;
	}
}
