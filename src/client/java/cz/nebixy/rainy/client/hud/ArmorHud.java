package cz.nebixy.rainy.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import cz.nebixy.rainy.client.util.Draw;

/**
 * Armor Status: helma, brnění, kalhoty a boty v rámečku jako hotbar, i s výdrží.
 */
public class ArmorHud extends HudModule {
	private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int WIDTH = 82;
	private static final int HEIGHT = 22;

	private ItemStack[] sample;

	public ArmorHud() {
		super("armor", "Armor Status", "Tvoje brnění a jeho výdrž v rámečku.", true, 4, 201);
	}

	/** Výchozí místo: vlevo vedle hotbaru (a slotu druhé ruky), zarovnané se spodkem obrazovky. */
	@Override
	protected int[] anchorPosition(int screenWidth, int screenHeight) {
		int hotbarLeft = screenWidth / 2 - 91;
		int offhand = 29; // slot pro druhou ruku vlevo od hotbaru
		int w = width > 0 ? width : WIDTH;
		int h = height > 0 ? height : HEIGHT;
		return new int[]{hotbarLeft - offhand - 4 - w, screenHeight - h};
	}

	@Override
	protected int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		ItemStack[] stacks = new ItemStack[SLOTS.length];
		boolean any = false;
		LocalPlayer player = Minecraft.getInstance().player;
		for (int i = 0; i < SLOTS.length; i++) {
			stacks[i] = player == null ? ItemStack.EMPTY : player.getItemBySlot(SLOTS[i]);
			any |= !stacks[i].isEmpty();
		}

		if (!any) {
			if (!editor) {
				return new int[]{WIDTH, HEIGHT}; // bez brnění nic nekreslíme, ať HUD nepřekáží
			}
			stacks = sample(); // v HUD editoru ukázka, ať je vidět, jak to vypadá
		}

		frame(g, x, y);
		for (int i = 0; i < stacks.length; i++) {
			if (stacks[i].isEmpty()) {
				continue;
			}
			int ix = x + 3 + i * 20;
			int iy = y + 3;
			g.item(stacks[i], ix, iy);
			g.itemDecorations(Draw.font(), stacks[i], ix, iy); // proužek výdrže
		}
		return new int[]{WIDTH, HEIGHT};
	}

	/** Rámeček se 4 sloty složený z kousků vanilla hotbaru (ten má 9 slotů, 182 x 22 px). */
	private static void frame(GuiGraphicsExtractor g, int x, int y) {
		// levý okraj + 1. slot
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, x, y, 21, 22);
		// další 3 sloty
		for (int i = 0; i < 3; i++) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 21, 0, x + 21 + i * 20, y, 20, 22);
		}
		// pravý okraj
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 181, 0, x + 81, y, 1, 22);
	}

	private ItemStack[] sample() {
		if (sample == null) {
			sample = new ItemStack[]{
					new ItemStack(Items.NETHERITE_HELMET),
					new ItemStack(Items.NETHERITE_CHESTPLATE),
					new ItemStack(Items.NETHERITE_LEGGINGS),
					new ItemStack(Items.NETHERITE_BOOTS)
			};
		}
		return sample;
	}
}
