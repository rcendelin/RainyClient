package cz.nebixy.rainy.client.module;

import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;

import cz.nebixy.rainy.client.RainyConfig;
import cz.nebixy.rainy.client.RainyKeys;
import cz.nebixy.rainy.client.util.Compat;

/**
 * Zoom s "woosh" efektem: drž C a obraz plynule vyjede dovnitř, pusť a zase vyjede ven.
 * Kolečkem myši během zoomu přiblížíš víc nebo míň. Citlivost myši se při zoomu sníží.
 *
 * FOV dělí CameraFovMixin, kolečko chytá MouseScrollMixin.
 */
public class ZoomModule extends Module {
	private static final float MIN_ZOOM = 1.5f;
	private static final float MAX_ZOOM = 30f;
	/** Jak rychle zoom vyjede (víc = rychlejší woosh). */
	private static final float SPEED = 14f;

	public static ZoomModule instance;

	private float zoomLevel = 4f;
	private float current = 1f;
	private long lastNanos;

	private boolean sensitivityApplied;
	private float sensitivityZoom;
	private Object savedSensitivity;
	/** Citlivost, kterou je potřeba vrátit, protože hra minule spadla během zoomu. */
	private Double pendingRestore;

	public ZoomModule() {
		super("zoom", "Zoom", "Drž C, plynule přiblíží. Kolečkem víc nebo míň.", Category.PVP, true);
		instance = this;
	}

	private boolean held() {
		Minecraft mc = Minecraft.getInstance();
		return isEnabled() && mc.player != null && mc.gui.screen() == null && RainyKeys.ZOOM.isDown();
	}

	/** Kolikrát se má FOV zmenšit (1 = bez zoomu). Volá se každý snímek. */
	public static float fovDivisor() {
		ZoomModule z = instance;
		if (z == null) {
			return 1f;
		}
		long now = System.nanoTime();
		float dt = z.lastNanos == 0 ? 0f : Math.min(0.1f, (now - z.lastNanos) / 1_000_000_000f);
		z.lastNanos = now;

		float target = z.held() ? z.zoomLevel : 1f;
		z.current += (target - z.current) * (1f - (float) Math.exp(-SPEED * dt));
		if (Math.abs(z.current - target) < 0.002f) {
			z.current = target;
		}
		return z.current;
	}

	/** Kolečko myši při zoomu. Vrací true, když se kolečko použilo (hotbar se pak nehne). */
	public static boolean onScroll(double amount) {
		ZoomModule z = instance;
		if (z == null || amount == 0 || !z.held()) {
			return false;
		}
		z.zoomLevel *= amount > 0 ? 1.25f : 0.8f;
		z.zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z.zoomLevel));
		return true;
	}

	@Override
	public void onTick(Minecraft mc) {
		if (pendingRestore != null && mc.options != null) {
			Compat.setOption("sensitivity", pendingRestore);
			pendingRestore = null;
			RainyConfig.save();
		}
		boolean held = held();
		if (held && (!sensitivityApplied || sensitivityZoom != zoomLevel)) {
			applySensitivity();
		} else if (!held && sensitivityApplied) {
			restoreSensitivity();
		}
	}

	/** Při zoomu snížit citlivost myši, ať se dá přesně mířit. */
	private void applySensitivity() {
		if (!sensitivityApplied) {
			savedSensitivity = Compat.getOption("sensitivity");
		}
		if (savedSensitivity instanceof Double s) {
			// Minecraft počítá rychlost myši jako (s * 0.6 + 0.2)^3, proto třetí odmocnina
			double f = s * 0.6 + 0.2;
			double scaled = f / Math.cbrt(zoomLevel);
			Compat.setOption("sensitivity", Math.max(0.0, Math.min(1.0, (scaled - 0.2) / 0.6)));
		}
		boolean first = !sensitivityApplied;
		sensitivityApplied = true;
		sensitivityZoom = zoomLevel;
		if (first) {
			RainyConfig.save(); // zapíše původní citlivost, kdyby hra během zoomu spadla
		}
	}

	private void restoreSensitivity() {
		if (savedSensitivity != null) {
			Compat.setOption("sensitivity", savedSensitivity);
		}
		sensitivityApplied = false;
		savedSensitivity = null;
		RainyConfig.save();
	}

	/** Vrátí citlivost, když se hra vypíná uprostřed zoomu. */
	public void forceStop() {
		if (sensitivityApplied) {
			restoreSensitivity();
		}
		current = 1f;
	}

	@Override
	protected void onDisable() {
		forceStop();
	}

	@Override
	public void save(JsonObject json) {
		super.save(json);
		json.addProperty("level", zoomLevel);
		if (sensitivityApplied && savedSensitivity instanceof Double d) {
			json.addProperty("restoreSensitivity", d);
		}
	}

	@Override
	public void load(JsonObject json) {
		super.load(json);
		if (json.has("level")) {
			zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, json.get("level").getAsFloat()));
		}
		pendingRestore = json.has("restoreSensitivity") ? json.get("restoreSensitivity").getAsDouble() : null;
	}
}
