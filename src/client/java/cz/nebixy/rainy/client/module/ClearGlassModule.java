package cz.nebixy.rainy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

/**
 * Neviditelné sklo: sklo se nekreslí, je jako vzduch. Když na něj míříš kurzorem,
 * uvidíš normální černý rámeček (jako u bariéry). Schování dělá ClearGlassMixin.
 */
public class ClearGlassModule extends Module {
	/** Čte ho ClearGlassMixin při stavění chunků. */
	public static volatile boolean active;

	public ClearGlassModule() {
		super("clear_glass", "Neviditelné sklo", "Sklo není vidět, je jako vzduch. Kurzorem uvidíš černý rámeček.", Category.VISUAL, false);
	}

	/** Sklo, barevné sklo, tónované sklo a skleněné tabule. Mřížky a měděné mřížky ne. */
	public static boolean isGlass(Block block) {
		return block == Blocks.GLASS
				|| block == Blocks.TINTED_GLASS
				|| block == Blocks.GLASS_PANE
				|| block instanceof StainedGlassBlock
				|| block instanceof StainedGlassPaneBlock;
	}

	@Override
	protected void onEnable() {
		active = true;
		reloadChunks();
	}

	@Override
	protected void onDisable() {
		active = false;
		reloadChunks();
	}

	@Override
	public void afterLoad() {
		active = isEnabled();
	}

	/** Překreslí chunky (jako F3 + A), ať se změna projeví hned. */
	private static void reloadChunks() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.level != null) {
			mc.levelExtractor.allChanged();
		}
	}
}
