package cz.nebixy.rainy.client.hud;

import com.google.gson.JsonObject;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import cz.nebixy.rainy.client.module.Category;
import cz.nebixy.rainy.client.module.Module;
import cz.nebixy.rainy.client.util.Draw;
import cz.nebixy.rainy.client.util.Theme;

/**
 * Modul, který se kreslí na obrazovku. Pozice jde měnit v HUD editoru.
 */
public abstract class HudModule extends Module {
	/** Globální nastavení: kreslit tmavé pozadí pod HUD moduly. */
	public static boolean background = true;

	public int x;
	public int y;
	private final int defaultX;
	private final int defaultY;

	/** Zvětšení modulu (1.0 = 100 %). Mění se kolečkem myši v HUD editoru. */
	public float scale = 1f;

	/**
	 * Přilepený na výchozí místo (třeba vedle hotbaru), které se přepočítá podle velikosti okna.
	 * Jakmile modul v HUD editoru přetáhneš, přestane být přilepený a zůstane, kam ho dáš.
	 */
	public boolean anchored;
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 3f;

	/** Velikost z posledního vykreslení už i se zvětšením (HUD editor podle ní pozná, kam se kliklo). */
	public int width = 40;
	public int height = 12;
	/** Kde se modul naposledy opravdu vykreslil (po oříznutí na obrazovku). */
	public int lastX;
	public int lastY;

	protected HudModule(String id, String name, String description, boolean enabledByDefault, int defaultX, int defaultY) {
		super(id, name, description, Category.HUD, enabledByDefault);
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.x = defaultX;
		this.y = defaultY;
		this.anchored = anchorPosition(400, 300) != null;
	}

	/**
	 * Vykreslí modul na pozici (x, y) a vrátí jeho velikost jako {šířka, výška}.
	 *
	 * @param editor true v HUD editoru (můžeš ukázat ukázková data)
	 */
	protected abstract int[] draw(GuiGraphicsExtractor g, int x, int y, boolean editor);

	/** Vykreslí modul ve správné velikosti tak, aby nikdy nevylezl z obrazovky. */
	public final void render(GuiGraphicsExtractor g, boolean editor) {
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		int px = x;
		int py = y;
		int[] anchor = anchored ? anchorPosition(sw, sh) : null;
		if (anchor != null) {
			px = anchor[0];
			py = anchor[1];
		}
		int drawX = Math.max(0, Math.min(px, sw - width));
		int drawY = Math.max(0, Math.min(py, sh - height));

		g.pose().pushMatrix();
		g.pose().translate(drawX, drawY);
		g.pose().scale(scale);
		int[] size = draw(g, 0, 0, editor);
		g.pose().popMatrix();

		width = Math.round(size[0] * scale);
		height = Math.round(size[1] * scale);
		lastX = drawX;
		lastY = drawY;
	}

	/** Změní velikost o krok (kladný = větší). Vrací novou velikost. */
	public float changeScale(int steps) {
		float next = Math.round((scale + steps * 0.1f) * 10f) / 10f;
		scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, next));
		return scale;
	}

	/** Klasický textový modul: "POPISEK hodnota" v boxu. */
	protected int[] drawLabel(GuiGraphicsExtractor g, int x, int y, String label, String value) {
		int pad = 4;
		int labelW = Draw.width(label);
		int w = pad + labelW + 4 + Draw.width(value) + pad;
		int h = Draw.lineHeight() + pad * 2 - 2;
		if (background) {
			Draw.rect(g, x, y, x + w, y + h, Theme.HUD_BG, 2);
		}
		Draw.text(g, label, x + pad, y + pad, Theme.accent());
		Draw.text(g, value, x + pad + labelW + 4, y + pad, Theme.TEXT);
		return new int[]{w, h};
	}

	/** Víc řádků pod sebou, každý řádek je {popisek, hodnota}. */
	protected int[] drawLines(GuiGraphicsExtractor g, int x, int y, String[][] lines) {
		int pad = 4;
		int labelW = 0;
		int valueW = 0;
		for (String[] line : lines) {
			labelW = Math.max(labelW, Draw.width(line[0]));
			valueW = Math.max(valueW, Draw.width(line[1]));
		}
		int lineH = Draw.lineHeight() + 1;
		int w = pad + labelW + 4 + valueW + pad;
		int h = pad * 2 + lineH * lines.length - 2;
		if (background) {
			Draw.rect(g, x, y, x + w, y + h, Theme.HUD_BG, 2);
		}
		for (int i = 0; i < lines.length; i++) {
			int ly = y + pad + i * lineH;
			Draw.text(g, lines[i][0], x + pad, ly, Theme.accent());
			Draw.text(g, lines[i][1], x + pad + labelW + 4, ly, Theme.TEXT);
		}
		return new int[]{w, h};
	}

	/**
	 * Výchozí místo spočítané podle velikosti obrazovky, nebo null (pak se používá pevné x, y).
	 * width a height jsou velikost modulu z posledního vykreslení.
	 */
	protected int[] anchorPosition(int screenWidth, int screenHeight) {
		return null;
	}

	/** Přestane být přilepený a zůstane na místě, kde je teď vidět (volá HUD editor při tažení). */
	public void detach() {
		anchored = false;
		x = lastX;
		y = lastY;
	}

	public void resetPosition() {
		anchored = anchorPosition(400, 300) != null;
		x = defaultX;
		y = defaultY;
		scale = 1f;
	}

	@Override
	public void save(JsonObject json) {
		super.save(json);
		json.addProperty("x", x);
		json.addProperty("y", y);
		json.addProperty("scale", scale);
		json.addProperty("anchored", anchored);
	}

	@Override
	public void load(JsonObject json) {
		super.load(json);
		if (json.has("x")) {
			x = json.get("x").getAsInt();
		}
		if (json.has("y")) {
			y = json.get("y").getAsInt();
		}
		// Starší config "anchored" nemá: moduly s výchozím místem se přilepí
		anchored = json.has("anchored") ? json.get("anchored").getAsBoolean() : anchorPosition(400, 300) != null;
		if (json.has("scale")) {
			scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, json.get("scale").getAsFloat()));
		}
	}
}
