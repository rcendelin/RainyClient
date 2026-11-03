'use strict';
// Java od Mojangu (stejná, jakou používá normální launcher). Pro 26.3 je to java-runtime-epsilon (Java 25).

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const paths = require('./paths');
const net = require('./net');

const RUNTIME_INDEX = 'https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json';

function platformKey() {
  if (process.platform === 'win32') {
    return { x64: 'windows-x64', ia32: 'windows-x86', arm64: 'windows-arm64' }[process.arch] || 'windows-x64';
  }
  if (process.platform === 'darwin') {
    return process.arch === 'arm64' ? 'mac-os-arm64' : 'mac-os';
  }
  return process.arch === 'ia32' ? 'linux-i386' : 'linux';
}

function javaExecutable(dir) {
  if (process.platform === 'win32') return path.join(dir, 'bin', 'javaw.exe');
  if (process.platform === 'darwin') return path.join(dir, 'jre.bundle', 'Contents', 'Home', 'bin', 'java');
  return path.join(dir, 'bin', 'java');
}

/**
 * Zajistí, že je Java stažená. Vrátí cestu k javaw.exe.
 * onProgress({ done, total, bytes, totalBytes })
 */
async function ensureJava(component, onProgress) {
  const dir = path.join(paths.runtime, component);
  const marker = path.join(dir, '.rainy-manifest');
  const exe = javaExecutable(dir);

  let index;
  try {
    index = await net.fetchJsonCached(RUNTIME_INDEX, path.join(paths.cache, 'java-runtime-all.json'));
  } catch (e) {
    if (fs.existsSync(exe)) return exe; // offline, ale Java už je
    throw e;
  }
  const entry = index[platformKey()]?.[component]?.[0];
  if (!entry) {
    if (fs.existsSync(exe)) return exe;
    throw new Error(`Mojang nemá Javu "${component}" pro tenhle systém (${platformKey()}).`);
  }

  // Už stažená a stejná verze? Pak nic nekontrolujeme, ať je start rychlý.
  if (fs.existsSync(exe) && fs.existsSync(marker)) {
    const have = (await fsp.readFile(marker, 'utf8')).trim();
    if (have === entry.manifest.sha1) return exe;
  }

  const manifest = await net.fetchJson(entry.manifest.url);
  const tasks = [];
  const links = [];
  for (const [rel, info] of Object.entries(manifest.files)) {
    const target = path.join(dir, rel);
    if (info.type === 'directory') {
      await fsp.mkdir(target, { recursive: true });
    } else if (info.type === 'file') {
      const raw = info.downloads.raw;
      tasks.push({ url: raw.url, dest: target, sha1: raw.sha1, size: raw.size, executable: info.executable });
    } else if (info.type === 'link') {
      links.push({ target, to: info.target });
    }
  }
  await net.downloadAll(tasks, { concurrency: 12, onProgress });

  if (process.platform !== 'win32') {
    for (const l of links) {
      await fsp.rm(l.target, { force: true });
      await fsp.mkdir(path.dirname(l.target), { recursive: true });
      await fsp.symlink(l.to, l.target).catch(() => {});
    }
  }
  await fsp.writeFile(marker, entry.manifest.sha1);
  return exe;
}

module.exports = { ensureJava, javaExecutable, platformKey };
