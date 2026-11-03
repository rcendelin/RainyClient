'use strict';
// Stahování: JSONy a soubory s kontrolou SHA1, víc najednou, s opakováním při chybě.

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const crypto = require('crypto');
const { Readable } = require('stream');
const { pipeline } = require('stream/promises');

const USER_AGENT = 'RainyLauncher/1.0 (github.com/nebixy)';

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function fetchWithRetry(url, options = {}, tries = 4) {
  let lastError;
  for (let i = 0; i < tries; i++) {
    try {
      const { timeout = 20000, ...rest } = options;
      const res = await fetch(url, {
        ...rest,
        signal: rest.signal || AbortSignal.timeout(timeout),
        headers: { 'User-Agent': USER_AGENT, ...(rest.headers || {}) },
      });
      if (res.status >= 500 || res.status === 429) {
        throw new Error(`Server vrátil ${res.status} (${url})`);
      }
      return res;
    } catch (e) {
      lastError = e;
      await sleep(500 * (i + 1) * (i + 1));
    }
  }
  throw lastError;
}

async function fetchJson(url, options) {
  const res = await fetchWithRetry(url, options);
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    const err = new Error(`HTTP ${res.status} pro ${url}`);
    err.status = res.status;
    err.body = body;
    throw err;
  }
  return res.json();
}

/** Stáhne JSON a uloží ho na disk. Když není internet, použije uloženou verzi. */
async function fetchJsonCached(url, file) {
  try {
    const data = await fetchJson(url);
    await fsp.mkdir(path.dirname(file), { recursive: true });
    await fsp.writeFile(file, JSON.stringify(data));
    return data;
  } catch (e) {
    if (fs.existsSync(file)) {
      return JSON.parse(await fsp.readFile(file, 'utf8'));
    }
    throw e;
  }
}

async function sha1File(file) {
  const hash = crypto.createHash('sha1');
  await pipeline(fs.createReadStream(file), hash);
  return hash.digest('hex');
}

/** Je soubor už stažený? (velikost sedí, a když chceme, i SHA1) */
async function isValid(file, size, sha1, deep) {
  try {
    const st = await fsp.stat(file);
    if (!st.isFile()) return false;
    if (size != null && st.size !== size) return false;
    if (deep && sha1) return (await sha1File(file)) === sha1;
    return true;
  } catch {
    return false;
  }
}

/**
 * Stáhne jeden soubor. task = { url, dest, sha1?, size?, executable? }
 * onBytes(n) se volá průběžně kvůli progress baru.
 */
async function downloadFile(task, onBytes) {
  await fsp.mkdir(path.dirname(task.dest), { recursive: true });
  const part = task.dest + '.part';
  let lastError;
  for (let attempt = 0; attempt < 4; attempt++) {
    let received = 0;
    // Když 30 s nepřijdou žádná data, zkusit znovu
    const controller = new AbortController();
    let stall = setTimeout(() => controller.abort(new Error('Stahování se zaseklo')), 30000);
    const kick = () => {
      clearTimeout(stall);
      stall = setTimeout(() => controller.abort(new Error('Stahování se zaseklo')), 30000);
    };
    try {
      const res = await fetchWithRetry(task.url, { signal: controller.signal }, 2);
      if (!res.ok) throw new Error(`HTTP ${res.status} pro ${task.url}`);
      const hash = crypto.createHash('sha1');
      const counter = new (require('stream').Transform)({
        transform(chunk, _enc, cb) {
          kick();
          received += chunk.length;
          hash.update(chunk);
          if (onBytes) onBytes(chunk.length);
          cb(null, chunk);
        },
      });
      await pipeline(Readable.fromWeb(res.body), counter, fs.createWriteStream(part));
      const got = hash.digest('hex');
      if (task.sha1 && got !== task.sha1) {
        throw new Error(`Špatný soubor (SHA1 nesedí): ${path.basename(task.dest)}`);
      }
      clearTimeout(stall);
      await fsp.rename(part, task.dest);
      if (task.executable && process.platform !== 'win32') {
        await fsp.chmod(task.dest, 0o755);
      }
      return;
    } catch (e) {
      clearTimeout(stall);
      lastError = e;
      if (onBytes && received) onBytes(-received);
      await fsp.rm(part, { force: true });
      await sleep(400 * (attempt + 1));
    }
  }
  throw lastError;
}

/**
 * Stáhne hodně souborů najednou. Přeskočí ty, co už jsou hotové.
 * onProgress({ done, total, bytes, totalBytes })
 */
async function downloadAll(tasks, { concurrency = 16, deep = false, onProgress } = {}) {
  const todo = [];
  for (const t of tasks) {
    if (!(await isValid(t.dest, t.size, t.sha1, deep))) todo.push(t);
  }
  const total = todo.length;
  const totalBytes = todo.reduce((s, t) => s + (t.size || 0), 0);
  let done = 0;
  let bytes = 0;
  let lastEmit = 0;
  const emit = (force) => {
    const now = Date.now();
    if (!onProgress || (!force && now - lastEmit < 80)) return;
    lastEmit = now;
    onProgress({ done, total, bytes, totalBytes });
  };
  emit(true);

  let index = 0;
  const worker = async () => {
    while (index < todo.length) {
      const t = todo[index++];
      await downloadFile(t, (n) => {
        bytes += n;
        emit(false);
      });
      done++;
      emit(false);
    }
  };
  await Promise.all(Array.from({ length: Math.min(concurrency, todo.length) }, worker));
  emit(true);
  return { downloaded: total };
}

module.exports = { fetchJson, fetchJsonCached, fetchWithRetry, downloadFile, downloadAll, sha1File, isValid };
