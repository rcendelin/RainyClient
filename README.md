# Rainy Client

PvP client pro Minecraft Java 26.3 (Fabric mod) a k němu **Rainy Launcher**, který stáhne a spustí hru s Rainy Clientem a FPS mody.

## Co je kde

| Složka | Co to je |
|---|---|
| `src/main`, `src/client` | Rainy Client (Java, Fabric). Moduly, HUD, menu, mixiny. |
| `src/client/java/.../module` | Jednotlivé moduly (Zoom, FPS Boost, Potato Graphics, ...) |
| `src/client/java/.../mixin` | Úpravy Minecraftu (fullbright, srdíčka nad hráči, neviditelné sklo, ...) |
| `src/client/java/.../gui` | Rainy Settings menu a HUD editor |
| `launcher/` | Rainy Launcher (Electron, JavaScript) |

## Moduly

- **HUD:** FPS, Ping, CPS, Keystrokes, Armor Status, Potion Timer, Souřadnice, Hodiny
- **PvP:** Toggle Sprint, Zoom (plynulý, kolečkem víc nebo míň), Srdíčka nad hráči
- **Vizuál:** Fullbright, Neviditelné sklo, Shulker Preview, Ikonka Rainy před jménem
- **FPS:** FPS Boost, Potato Graphics, FPS test (30 s, průměr a 1% low)
- **Nastavení:** barva clientu, pozadí HUD, reset HUD, klávesy

Menu se otevírá **pravým Shiftem** nebo v ESC menu tlačítkem **Rainy Settings**.

## Spuštění ve vývoji (IntelliJ IDEA)

1. **File → Open** a vybrat tuhle složku. Poprvé Gradle stahuje Minecraft, trvá to pár minut.
2. Gradle JVM: **Java 25** (File → Settings → Build Tools → Gradle).
3. Vpravo nahoře **Minecraft Client** a ▶.

Ve vývoji se spolu s Rainy pouští i FPS mody (Sodium, Lithium, ...) z `build.gradle`.

## Launcher

Viz [`launcher/README.md`](launcher/README.md). Krátce: `gradlew build`, jar z `build/libs` zkopírovat do `launcher/bundled/rainy-client.jar`, pak v `launcher/` `npm install` a `npm run pack:win`.

## Technika

- Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0, Java 25
- Mody pro launcher se stahují z Modrinthu (`launcher/config.json`)
- Přihlášení Microsoft v launcheru čeká na schválení aplikace od Microsoftu. Do té doby se hra spouští přes normální Minecraft Launcher (profil Rainy Client).
