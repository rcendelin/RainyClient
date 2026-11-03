'use strict';
// Záložní režim: dokud Microsoft neschválí přihlášení, hra se spouští přes normální Minecraft Launcher.
// Vytvoříme v něm profil "Rainy Client", který používá naši složku s mody.

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const { spawn } = require('child_process');
const paths = require('./paths');

const PROFILE_KEY = 'rainy-client';
const STORE_APP = 'shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft';

async function readJson(file, fallback) {
  try {
    return JSON.parse(await fsp.readFile(file, 'utf8'));
  } catch {
    return fallback;
  }
}

/** Zapíše Fabric verzi a profil "Rainy Client" do normálního launcheru. */
async function setupProfile({ versionId, fabricJson, javaArgs, iconDataUrl }) {
  const mc = paths.officialMinecraft;
  const vdir = path.join(mc, 'versions', versionId);
  await fsp.mkdir(vdir, { recursive: true });
  await fsp.writeFile(path.join(vdir, `${versionId}.json`), JSON.stringify(fabricJson, null, 2));
  // Prázdný jar, stejně jako to dělá Fabric installer
  const jar = path.join(vdir, `${versionId}.jar`);
  if (!fs.existsSync(jar)) await fsp.writeFile(jar, '');

  const file = path.join(mc, 'launcher_profiles.json');
  const data = await readJson(file, null);
  if (data && fs.existsSync(file)) {
    // Záloha, kdyby se něco pokazilo
    await fsp.copyFile(file, file + '.rainy-backup').catch(() => {});
  }
  const lp = data || { profiles: {}, settings: {}, version: 3 };
  lp.profiles = lp.profiles || {};
  const now = new Date().toISOString();
  const old = lp.profiles[PROFILE_KEY] || {};
  lp.profiles[PROFILE_KEY] = {
    ...old,
    name: 'Rainy Client',
    type: 'custom',
    created: old.created || now,
    lastUsed: now,
    icon: iconDataUrl || old.icon || 'Furnace',
    lastVersionId: versionId,
    gameDir: paths.game,
    javaArgs,
  };
  await fsp.mkdir(mc, { recursive: true });
  await fsp.writeFile(file, JSON.stringify(lp, null, 2));
}

/** Otevře normální Minecraft Launcher. Vrátí 'exe', 'store' nebo null. */
function openOfficialLauncher() {
  if (process.platform !== 'win32') return null;
  const env = process.env;
  const candidates = [
    path.join(env['ProgramFiles(x86)'] || 'C:\\Program Files (x86)', 'Minecraft Launcher', 'MinecraftLauncher.exe'),
    path.join(env.ProgramFiles || 'C:\\Program Files', 'Minecraft Launcher', 'MinecraftLauncher.exe'),
    path.join(env.LOCALAPPDATA || '', 'Programs', 'Minecraft Launcher', 'MinecraftLauncher.exe'),
    path.join(env['ProgramFiles(x86)'] || 'C:\\Program Files (x86)', 'Minecraft', 'MinecraftLauncher.exe'),
  ];
  for (const exe of candidates) {
    if (exe && fs.existsSync(exe)) {
      spawn(exe, [], { detached: true, stdio: 'ignore' }).unref();
      return 'exe';
    }
  }
  // Verze z Microsoft Store / Xbox aplikace
  try {
    spawn('explorer.exe', [STORE_APP], { detached: true, stdio: 'ignore' }).unref();
    return 'store';
  } catch {
    return null;
  }
}

/** Při prvním spuštění zkopíruje nastavení a servery z normálního Minecraftu. */
async function importFromMinecraft() {
  const copied = [];
  await fsp.mkdir(paths.game, { recursive: true });
  for (const name of ['options.txt', 'servers.dat']) {
    const src = path.join(paths.officialMinecraft, name);
    const dest = path.join(paths.game, name);
    if (fs.existsSync(src) && !fs.existsSync(dest)) {
      await fsp.copyFile(src, dest);
      copied.push(name);
    }
  }
  return copied;
}

module.exports = { setupProfile, openOfficialLauncher, importFromMinecraft, PROFILE_KEY };
