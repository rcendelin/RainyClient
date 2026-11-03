'use strict';
// Mody: stáhne zapnuté mody z Modrinthu, přidá Rainy Client a uklidí vypnuté.
// Mody, které si tam hráč dá sám, nikdy nemaže.

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const paths = require('./paths');
const net = require('./net');

const MODRINTH = 'https://api.modrinth.com/v2';
const MANAGED_FILE = '.rainy-managed.json';

/** Které mody jsou zapnuté (povinné vždy, ostatní podle nastavení, plus závislosti). */
function enabledMods(config, settings) {
  const byId = Object.fromEntries(config.mods.map((m) => [m.id, m]));
  const on = new Set();
  for (const m of config.mods) {
    if (m.library) continue;
    const userValue = settings.mods?.[m.id];
    if (m.required || userValue !== false) on.add(m.id);
  }
  // Sodium Extra bez Sodia nejede
  for (const id of [...on]) {
    for (const need of byId[id].needs || []) {
      if (!on.has(need)) on.delete(id);
    }
  }
  // Knihovny (třeba Cloth Config) přidat, když je potřebuje některý zapnutý mod
  for (const id of [...on]) {
    for (const dep of byId[id].deps || []) on.add(dep);
  }
  return config.mods.filter((m) => on.has(m.id));
}

async function resolveModrinth(mod) {
  const cacheFile = path.join(paths.cache, 'mods', `${mod.modrinth}@${mod.version}.json`.replace(/[^\w.@+-]/g, '_'));
  const v = await net.fetchJsonCached(
    `${MODRINTH}/project/${encodeURIComponent(mod.modrinth)}/version/${encodeURIComponent(mod.version)}`,
    cacheFile,
  );
  const file = v.files.find((f) => f.primary) || v.files[0];
  return { url: file.url, filename: file.filename, sha1: file.hashes.sha1, size: file.size };
}

function bundledDir() {
  // V nainstalované aplikaci: resources/bundled, při vývoji: ./bundled
  const packed = process.resourcesPath ? path.join(process.resourcesPath, 'bundled') : null;
  if (packed && fs.existsSync(packed)) return packed;
  return path.join(__dirname, '..', '..', 'bundled');
}

async function install(config, settings, { onStage, onProgress } = {}) {
  onStage?.('Připravuju mody');
  const modsDir = path.join(paths.game, 'mods');
  await fsp.mkdir(modsDir, { recursive: true });

  const managedPath = path.join(modsDir, MANAGED_FILE);
  let managed = [];
  try {
    managed = JSON.parse(await fsp.readFile(managedPath, 'utf8'));
  } catch {}

  const wanted = [];
  const tasks = [];
  for (const mod of enabledMods(config, settings)) {
    if (mod.bundled) {
      const src = path.join(bundledDir(), mod.bundled);
      if (!fs.existsSync(src)) throw new Error(`V launcheru chybí ${mod.bundled}.`);
      const dest = path.join(modsDir, mod.bundled);
      const same = fs.existsSync(dest) && (await net.sha1File(dest)) === (await net.sha1File(src));
      if (!same) await fsp.copyFile(src, dest);
      wanted.push(mod.bundled);
    } else if (mod.modrinth) {
      const f = await resolveModrinth(mod);
      tasks.push({ url: f.url, dest: path.join(modsDir, f.filename), sha1: f.sha1, size: f.size });
      wanted.push(f.filename);
    }
  }

  onStage?.('Stahuju mody');
  await net.downloadAll(tasks, { concurrency: 6, deep: true, onProgress });

  // Smazat jen to, co jsme tam dali my a už to není zapnuté (nebo je to stará verze)
  for (const name of managed) {
    if (!wanted.includes(name)) {
      await fsp.rm(path.join(modsDir, name), { force: true });
    }
  }
  await fsp.writeFile(managedPath, JSON.stringify(wanted, null, 2));
  return wanted;
}

module.exports = { install, enabledMods };
