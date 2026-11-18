package cz.nebixy.rainy.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

import cz.nebixy.rainy.RainyClient;
import cz.nebixy.rainy.client.module.Module;
import cz.nebixy.rainy.client.module.ModuleManager;

/**
 * Ukládá zapnuté moduly a pozice HUD do .minecraft/config/rainy-client.json
 */
public final class RainyConfig {
	private RainyConfig() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("rainy-client.json");
	}

	public static void load() {
		JsonObject root = new JsonObject();
		Path path = file();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				JsonObject parsed = GSON.fromJson(reader, JsonObject.class);
				if (parsed != null) {
					root = parsed;
				}
			} catch (Exception e) {
				RainyClient.LOGGER.error("[Rainy] Config nejde načíst, používám výchozí", e);
			}
		}
		JsonObject modules = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
		for (Module m : ModuleManager.all()) {
			try {
				JsonObject json = modules.has(m.id) && modules.get(m.id).isJsonObject() ? modules.getAsJsonObject(m.id) : new JsonObject();
				m.load(json);
			} catch (Exception e) {
				// Rozbitá hodnota u jednoho modulu nesmí shodit celý config
				RainyClient.LOGGER.warn("[Rainy] Nastavení modulu {} je poškozené, používám výchozí", m.id, e);
				m.load(new JsonObject());
			}
		}
		for (Module m : ModuleManager.all()) {
			try {
				m.afterLoad();
			} catch (Exception e) {
				RainyClient.LOGGER.warn("[Rainy] Modul {} se nepodařilo zapnout", m.id, e);
			}
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", RainyClient.VERSION);
		JsonObject modules = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject json = new JsonObject();
			m.save(json);
			modules.add(m.id, json);
		}
		root.add("modules", modules);
		try {
			Path path = file();
			Files.createDirectories(path.getParent());
			// Nejdřív do dočasného souboru a pak přesunout, ať se config nerozbije, když hra spadne při ukládání
			Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
			try {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			RainyClient.LOGGER.error("[Rainy] Config nejde uložit", e);
		}
	}
}
