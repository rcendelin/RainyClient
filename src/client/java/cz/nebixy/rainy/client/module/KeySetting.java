package cz.nebixy.rainy.client.module;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Řádek v nastavení pro klávesy clientu (menu, zoom, HUD editor).
 * Mění rovnou klávesu v Ovládání Minecraftu, takže se uloží do jeho nastavení.
 */
public class KeySetting extends Module {
	private final KeyMapping mapping;

	public KeySetting(String id, String name, String description, KeyMapping mapping) {
		super(id, name, description, Category.SETTINGS, true);
		this.mapping = mapping;
	}

	/** Kliknutí na kartu nic nepřepíná, klávesa se mění tlačítkem vpravo. */
	@Override
	public void onClick() {
	}

	@Override
	public boolean bindable() {
		return false;
	}

	public String keyName() {
		return mapping.getTranslatedKeyMessage().getString();
	}

	public void setKey(int key) {
		mapping.setKey(key < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYBOARD.getOrCreate(key));
		KeyMapping.resetMapping();
		Minecraft.getInstance().options.save();
	}

	@Override
	public void save(JsonObject json) {
	}

	@Override
	public void load(JsonObject json) {
	}

	@Override
	public void afterLoad() {
	}
}
