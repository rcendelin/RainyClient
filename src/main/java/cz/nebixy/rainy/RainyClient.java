package cz.nebixy.rainy;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RainyClient implements ModInitializer {
	public static final String MOD_ID = "rainy-client";
	public static final String NAME = "Rainy Client";
	public static final String VERSION = "1.0.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	@Override
	public void onInitialize() {
		// Rainy Client is client-only, everything happens in RainyClientClient.
	}
}
