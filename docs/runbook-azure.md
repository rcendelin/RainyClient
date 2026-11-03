# Runbook: Microsoft přihlášení pro Rainy Launcher (Azure)

Cíl: získat **Client ID** schválené pro Minecraft, aby se v Rainy Launcheru dalo přihlásit Microsoft účtem a hra se spouštěla přímo, bez normálního Minecraft Launcheru.

| | |
|---|---|
| Kdo | Dospělý s Microsoft účtem (Azure chce kartu a telefon kvůli ověření totožnosti) |
| Čas | cca 15 minut práce, pak čekání na schválení od Mojangu (dny až týdny) |
| Cena | 0 Kč. Žádné placené služby se nezapínají. |
| Azure prostředky | Žádné (žádné VM, úložiště ani resource group). Jen jedna registrace aplikace v Microsoft Entra ID (bezplatná úroveň). |
| Výstup | Application (client) ID, vložené do launcheru |

Do schválení launcher funguje dál v záložním režimu: připraví mody a spustí hru přes normální Minecraft Launcher (profil **Rainy Client**).

---

## 1. Azure účet

1. Otevřít <https://azure.microsoft.com/free> a přihlásit se Microsoft účtem.
2. Založit bezplatný účet (ověření telefonem, kartou a adresou).
3. **Nezapínat** pay-as-you-go ani žádné služby. Pro registraci aplikace stačí výchozí adresář (*Default Directory*), který vznikne automaticky.

> Proč je potřeba adresář: Microsoft zrušil starý portál, kde šlo aplikaci zaregistrovat osobním účtem bez tenantu. Registrace dnes musí patřit do nějakého adresáře v Entra ID.

## 2. Registrace aplikace

<https://portal.azure.com> → vyhledat **App registrations** → **New registration**

| Pole | Hodnota |
|---|---|
| Name | `Rainy Launcher` (nesmí obsahovat Minecraft, Mojang, Microsoft, Xbox) |
| Supported account types | **Personal Microsoft accounts only** |
| Redirect URI, platforma | **Public client/native (mobile & desktop)** |
| Redirect URI, adresa | `https://login.microsoftonline.com/common/oauth2/nativeclient` |

→ **Register**

## 3. Nastavení po registraci

1. **Authentication** → *Advanced settings* → **Allow public client flows: Yes** → **Save**.
2. Zkontrolovat, že pod *Mobile and desktop applications* je redirect URI z kroku 2.
3. **Nic dalšího se nenastavuje:**
   - žádný client secret ani certifikát (launcher je veřejný klient s PKCE),
   - žádné API permissions (scope `XboxLive.signin offline_access` si launcher vyžádá při přihlášení).
4. Na stránce **Overview** si zapsat:
   - **Application (client) ID**
   - **Directory (tenant) ID**

## 4. Žádost o povolení Minecraft API

Nové aplikace musí Mojang schválit, jinak Minecraft přihlášení vrací HTTP 403 *Invalid app registration*.

1. Formulář: <https://aka.ms/mce-reviewappid> (Minecraft Java, „App ID review“).
2. Vyplnit:
   - Application (client) ID a Directory (tenant) ID z kroku 3,
   - název aplikace `Rainy Launcher`,
   - kontaktní e-mail (stejný jako u Azure účtu),
   - popis, například:

     ```
     Rainy Launcher is a small, non-commercial launcher for Minecraft Java Edition, used by our family and a few friends. It signs players in with their Microsoft account and starts the game with Fabric mods. No account data leaves the user's PC.
     ```
3. Odeslat **jen jednou**. Mojang žádosti prochází zhruba jednou týdně, opakované odeslání nepomáhá. Odpověď přijde e-mailem.

## 5. Client ID do launcheru

Po schválení:

- **Na jednom PC:** Rainy Launcher → **Nastavení → Microsoft přihlášení** → vložit Client ID → vlevo dole **Přihlásit se**.
- **Pro všechny (doporučeno):** vložit Client ID do `launcher/config.json`:

  ```json
  "azureClientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
  ```

  a postavit novou verzi launcheru (`npm run pack:win`). Kamarádi pak nic vkládat nemusí. Client ID není tajné, v repozitáři být může.

## 6. Ověření

1. Launcher → **Přihlásit se** → otevře se okno Microsoftu → po přihlášení se vlevo dole ukáže jméno a hlava postavy.
2. Pod tlačítkem HRÁT je text *„Spustí se přímo jako …“*.
3. HRÁT → launcher stáhne Javu, Minecraft a mody a spustí hru. Na online serveru (třeba DonutSMP) to musí jít bez chyby přihlášení.

## Řešení problémů

| Chyba | Příčina | Řešení |
|---|---|---|
| `AADSTS7000218` nebo `AADSTS70002` (chce client_secret) | Aplikace není nastavená jako veřejný klient (redirect URI je pod *Web* místo *Mobile and desktop*, nebo jsou vypnuté public client flows) | Krok 2 a 3.1: redirect pod *Mobile and desktop applications*, Allow public client flows = Yes |
| `AADSTS50011` (redirect URI mismatch) | Chybí nebo nesedí redirect URI | Krok 2: přesně `https://login.microsoftonline.com/common/oauth2/nativeclient`, platforma Mobile and desktop |
| `AADSTS700016` (application not found) | Špatné Client ID nebo jiný typ účtů | Zkontrolovat Client ID a *Personal Microsoft accounts only* |
| Launcher: „Microsoft ještě neschválil Rainy Launcher“ (HTTP 403 *Invalid app registration*) | Mojang ještě neschválil | Počkat na e-mail z kroku 4, do té doby záložní režim |
| Xbox chyba `2148916238` | Dětský účet není v Microsoft rodině | Přidat účet do rodiny na <https://family.microsoft.com> |
| Xbox chyba `2148916233` | Účet nemá Xbox profil | Jednou se přihlásit na <https://minecraft.net> |
| „Tenhle účet nemá koupený Minecraft Java“ | Účet nevlastní hru | Přihlásit se účtem, který Minecraft Java má |

## Bezpečnost

- Launcher heslo nikdy nevidí, přihlášení probíhá v okně Microsoftu (OAuth 2.0 authorization code + PKCE).
- Na PC se ukládá jen refresh token, zašifrovaný přes Windows (Electron `safeStorage`, DPAPI), v `%APPDATA%\Rainy Launcher\account.json`. Odhlášení v launcheru ho smaže.
- Přihlášení lidé se **nestávají členy** vašeho adresáře v Entra ID. Adresář jen vlastní registraci aplikace.
- Přístup aplikace k účtu jde kdykoli zrušit na <https://account.live.com/consent/Manage>.

## Vrácení zpět

1. portal.azure.com → App registrations → Rainy Launcher → **Delete**.
2. V launcheru smazat Client ID (Nastavení) nebo ho odebrat z `launcher/config.json`.
3. Launcher se sám vrátí do záložního režimu přes Minecraft Launcher.

## Zdroje

- [minecraft-launcher-lib: Microsoft Login](https://minecraft-launcher-lib.readthedocs.io/en/stable/tutorial/microsoft_login.html)
- [lighty-auth: Microsoft application ID](https://docs.rs/crate/lighty-auth/26.9.2/source/docs/application-id-microsoft.md)
- [Horizon launcher: Microsoft account setup](https://raw.githubusercontent.com/horizon-rust/horizon/master/docs/microsoft-account.md)
- [Microsoft Q&A: XboxLive.signin pro Minecraft launcher](https://learn.microsoft.com/en-gb/answers/questions/5768276/how-to-get-xboxlive-signin-permission-for-azure-ap)
