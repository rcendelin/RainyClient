'use strict';
// Minecraft + Fabric: stáhne verzi, knihovny, assety a připraví vše pro spuštění.

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const paths = require('./paths');
const net = require('./net');
const { allowed, mavenPath, libraryKey } = require('./rules');

const VERSION_MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const FABRIC_META = 'https://meta.fabricmc.net/v2';
const ASSETS_URL = 'https://resources.download.minecraft.net';

function fabricId(mc, loader) {
  return `fabric-loader-${loader}-${mc}`;
}

async function readJsonIfExists(file) {
  try {
    return JSON.parse(await fsp.readFile(file, 'utf8'));
  } catch {
    return null;
  }
}

/** Vanilla JSON verze (např. 26.3). Uloží se, takže pak jde spustit i bez internetu. */
async function loadVanilla(mc) {
  const file = path.join(paths.versions, mc, `${mc}.json`);
  const local = await readJsonIfExists(file);
  try {
    const manifest = await net.fetchJsonCached(VERSION_MANIFEST, path.join(paths.cache, 'version_manifest_v2.json'));
    const entry = manifest.versions.find((v) => v.id === mc);
    if (!entry) throw new Error(`Minecraft ${mc} neexistuje.`);
    if (local && (await net.sha1File(file)) === entry.sha1) return local;
    await net.downloadFile({ url: entry.url, dest: file, sha1: entry.sha1 });
    return JSON.parse(await fsp.readFile(file, 'utf8'));
  } catch (e) {
    if (local) return local;
    throw e;
  }
}

/** Fabric profil (knihovny Fabric Loaderu + hlavní třída). */
async function loadFabric(mc, loader) {
  const id = fabricId(mc, loader);
  const file = path.join(paths.versions, id, `${id}.json`);
  const url = `${FABRIC_META}/versions/loader/${encodeURIComponent(mc)}/${encodeURIComponent(loader)}/profile/json`;
  return net.fetchJsonCached(url, file);
}

/** Spojí Fabric profil s vanilla verzí (inheritsFrom), jako to dělá normální launcher. */
function merge(child, parent) {
  const libs = [];
  const seen = new Set();
  for (const lib of [...(child.libraries || []), ...(parent.libraries || [])]) {
    const key = libraryKey(lib.name);
    if (seen.has(key)) continue;
    seen.add(key);
    libs.push(lib);
  }
  return {
    ...parent,
    id: child.id,
    mainClass: child.mainClass || parent.mainClass,
    libraries: libs,
    arguments: {
      game: [...(parent.arguments?.game || []), ...(child.arguments?.game || [])],
      jvm: [...(parent.arguments?.jvm || []), ...(child.arguments?.jvm || [])],
      'default-user-jvm': parent.arguments?.['default-user-jvm'] || [],
    },
  };
}

/** Z knihovny udělá úkol ke stažení (nebo null, když pro tenhle systém není). */
function libraryTask(lib) {
  if (!allowed(lib.rules)) return null;
  const art = lib.downloads?.artifact;
  if (art) {
    const rel = art.path || mavenPath(lib.name);
    return { url: art.url, dest: path.join(paths.libraries, rel), sha1: art.sha1, size: art.size };
  }
  // Fabric styl: jen jméno + adresa repozitáře
  const rel = mavenPath(lib.name);
  const base = (lib.url || 'https://libraries.minecraft.net/').replace(/\/?$/, '/');
  return { url: base + rel, dest: path.join(paths.libraries, rel), sha1: lib.sha1, size: lib.size };
}

/**
 * Připraví všechno pro spuštění. onStage(text), onProgress({done,total,bytes,totalBytes})
 * Vrátí "profil" pro launch.js.
 */
async function prepare(mc, loader, { onStage, onProgress } = {}) {
  onStage?.('Načítám verzi Minecraftu');
  const vanilla = await loadVanilla(mc);
  const fabric = await loadFabric(mc, loader);
  const version = merge(fabric, vanilla);

  // Knihovny + samotná hra
  onStage?.('Stahuju knihovny');
  const libTasks = version.libraries.map(libraryTask).filter(Boolean);
  const clientJar = path.join(paths.versions, mc, `${mc}.jar`);
  const c = vanilla.downloads.client;
  const jarTasks = [...libTasks, { url: c.url, dest: clientJar, sha1: c.sha1, size: c.size }];
  await net.downloadAll(jarTasks, { concurrency: 16, onProgress });

  // Logování (log4j config)
  let logging = null;
  const lc = vanilla.logging?.client;
  if (lc?.file) {
    const dest = path.join(paths.assets, 'log_configs', lc.file.id);
    await net.downloadAll([{ url: lc.file.url, dest, sha1: lc.file.sha1, size: lc.file.size }]);
    logging = lc.argument.replace('${path}', dest);
  }

  // Assety (zvuky, jazyky, textury)
  onStage?.('Stahuju zvuky a textury');
  const ai = vanilla.assetIndex;
  const indexFile = path.join(paths.assets, 'indexes', `${ai.id}.json`);
  await net.downloadAll([{ url: ai.url, dest: indexFile, sha1: ai.sha1, size: ai.size }]);
  const index = JSON.parse(await fsp.readFile(indexFile, 'utf8'));
  const assetTasks = [];
  const seen = new Set();
  for (const obj of Object.values(index.objects)) {
    if (seen.has(obj.hash)) continue;
    seen.add(obj.hash);
    const sub = obj.hash.slice(0, 2);
    assetTasks.push({
      url: `${ASSETS_URL}/${sub}/${obj.hash}`,
      dest: path.join(paths.assets, 'objects', sub, obj.hash),
      sha1: obj.hash,
      size: obj.size,
    });
  }
  await net.downloadAll(assetTasks, { concurrency: 24, onProgress });

  return {
    id: version.id,
    mc,
    mainClass: version.mainClass,
    type: vanilla.type || 'release',
    javaComponent: vanilla.javaVersion?.component || 'java-runtime-epsilon',
    classpath: [...libTasks.map((t) => t.dest), clientJar],
    arguments: version.arguments,
    assetIndex: ai.id,
    logging,
    fabricJson: fabric,
  };
}

module.exports = { prepare, fabricId, loadFabric, loadVanilla, merge, libraryTask };
