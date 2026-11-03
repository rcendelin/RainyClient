package cz.nebixy.rainy.client.util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import cz.nebixy.rainy.RainyClient;

/**
 * Ikonka Rainy (černobílé logo) před jménem hráčů, kteří používají Rainy Client.
 * Logo je jako znak  ve vlastním fontu rainy-client:icons.
 */
public final class RainyBadge {
	private RainyBadge() {
	}

	private static final FontDescription ICON_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath(RainyClient.MOD_ID, "icons"));

	/** Zapnuto/vypnuto v Nastavení (modul Ikonka Rainy). */
	public static volatile boolean enabled = true;

	/** Hráči, o kterých víme, že mají Rainy Client (zatím ty sám, později z Rainy serveru). */
	private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();

	public static MutableComponent icon() {
		return Component.literal("").setStyle(Style.EMPTY.withFont(ICON_FONT).withColor(0xFFFFFF));
	}

	/** Vrátí "[logo] jméno". */
	public static Component prefix(Component name) {
		return Component.empty().append(icon()).append(Component.literal(" ")).append(name);
	}

	public static void addUser(UUID uuid) {
		if (uuid != null) {
			USERS.add(uuid);
		}
	}

	public static boolean isRainy(UUID uuid) {
		if (uuid == null) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && uuid.equals(mc.player.getUUID())) {
			return true; // ty sám Rainy máš vždycky
		}
		return USERS.contains(uuid);
	}

	/** Patří tenhle řádek v TABu někomu s Rainy Clientem? */
	public static boolean isRainy(PlayerInfo info) {
		Minecraft mc = Minecraft.getInstance();
		if (info == null || mc.player == null) {
			return false;
		}
		if (info == mc.player.connection.getPlayerInfo(mc.player.getUUID())) {
			return true;
		}
		for (UUID uuid : USERS) {
			if (info == mc.player.connection.getPlayerInfo(uuid)) {
				return true;
			}
		}
		return false;
	}
}
