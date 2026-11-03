# Rainy Launcher

Launcher pro Rainy Client (Electron). Stáhne Javu, Minecraft 26.3, Fabric, Rainy Client a FPS mody a spustí hru.

## Jak to funguje

- **Bez přihlášení (teď):** HRÁT připraví mody do `%APPDATA%\.rainyclient\game`, vytvoří profil **Rainy Client** v normálním Minecraft Launcheru a otevře ho.
- **S přihlášením (až Microsoft schválí aplikaci):** do Nastavení → Microsoft přihlášení se vloží Client ID z Azure. Pak se přihlásíš v launcheru a hra se spouští přímo.

## Soubory

- `config.json`: verze Minecraftu, Fabricu a seznam modů (z Modrinthu)
- `bundled/rainy-client.jar`: Rainy Client, který se přibalí do launcheru
- `src/engine/`: stahování, Java, Minecraft, mody, přihlášení, spuštění
- `renderer/`: vzhled launcheru (HTML, CSS, JS)

## Nová verze Rainy Clientu

1. Postav mod (`gradlew build`) a nahraď `bundled/rainy-client.jar`.
2. V `package.json` zvyš `version`.
3. `npm install` a `npm run pack:win` → `dist/RainyLauncherSetup.exe`.

Hra, světy a nastavení jsou v `%APPDATA%\.rainyclient`, launcher sám v `%LOCALAPPDATA%\Programs\Rainy Launcher`.
