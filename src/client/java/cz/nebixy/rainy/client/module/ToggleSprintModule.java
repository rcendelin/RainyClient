package cz.nebixy.rainy.client.module;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Pořád sprintuješ, když jdeš dopředu. Nemusíš držet Ctrl.
 */
public class ToggleSprintModule extends Module {
	public ToggleSprintModule() {
		super("toggle_sprint", "Toggle Sprint", "Sprint pořád zapnutý, bez držení Ctrl.", Category.PVP, true);
	}

	@Override
	public void onTick(Minecraft mc) {
		if (mc.player == null) {
			return;
		}
		KeyMapping sprint = mc.options.keySprint;
		if (mc.options.keyUp.isDown() && !sprint.isDown()) {
			sprint.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		Minecraft.getInstance().options.keySprint.setDown(false);
	}
}
