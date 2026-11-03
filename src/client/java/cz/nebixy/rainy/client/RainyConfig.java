package cz.nebixy.rainy.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
			m.load(modules.has(m.id) ? modules.getAsJsonObject(m.id) : new JsonObject());
		}
		for (Module m : ModuleManager.all()) {
			m.afterLoad();
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
			Files.createDirectories(file().getParent());
			try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			RainyClient.LOGGER.error("[Rainy] Config nejde uložit", e);
		}
	}
}
