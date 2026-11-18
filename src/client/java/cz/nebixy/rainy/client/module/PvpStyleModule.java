package cz.nebixy.rainy.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * PvP Style: věci, které používají PvP hráči. Kliknutím se rozbalí a zapneš si jen ty, které chceš.
 */
public class PvpStyleModule extends Module {
	private final List<PvpOption> options = new ArrayList<>();
	/** Rozbalený v menu? */
	public boolean expanded;

	public PvpStyleModule() {
		super("pvp_style", "PvP Style", "Nastavení jako mají PvP hráči. Klikni a vyber si.", Category.PVP, true);
	}

	public <T extends PvpOption> T add(T option) {
		options.add(option);
		return option;
	}

	public List<PvpOption> options() {
		return Collections.unmodifiableList(options);
	}

	/** Kliknutí rozbalí/sbalí seznam. */
	@Override
	public void onClick() {
		expanded = !expanded;
	}

	@Override
	public boolean bindable() {
		return false;
	}

	@Override
	public String valueText() {
		int on = 0;
		for (PvpOption o : options) {
			if (o.isEnabled()) {
				on++;
			}
		}
		return on + "/" + options.size() + (expanded ? "  ▲" : "  ▼");
	}

	@Override
	public void save(JsonObject json) {
		json.addProperty("expanded", expanded);
	}

	@Override
	public void load(JsonObject json) {
		expanded = json.has("expanded") && json.get("expanded").getAsBoolean();
	}

	@Override
	public void afterLoad() {
	}
}
