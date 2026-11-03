'use strict';
// Rainy Launcher: hlavní proces (okno, přihlášení, stahování, spuštění hry).

const { app, BrowserWindow, ipcMain, shell, safeStorage } = require('electron');
const os = require('os');
const fs = require('fs');
const path = require('path');

const config = require('./config.json');
const paths = require('./src/engine/paths');
const minecraft = require('./src/engine/minecraft');
const java = require('./src/engine/java');
const mods = require('./src/engine/mods');
const launcher = require('./src/engine/launch');
const auth = require('./src/engine/auth');
const official = require('./src/engine/official');
const perf = require('./src/engine/perf');
const { Store } = require('./src/engine/store');

let win = null;
let store = null;
let busy = false;
let game = null;

if (!app.requestSingleInstanceLock()) {
  app.quit();
}
app.on('second-instance', () => {
  if (win) {
    if (win.isMinimized()) win.restore();
    win.show();
    win.focus();
  }
});

const crypto = {
  encrypt(str) {
    if (!str) return '';
    if (safeStorage.isEncryptionAvailable()) return 'enc:' + safeStorage.encryptString(str).toString('base64');
    return 'raw:' + Buffer.from(str).toString('base64');
  },
  decrypt(str) {
    if (!str) return '';
    if (str.startsWith('enc:')) return safeStorage.decryptString(Buffer.from(str.slice(4), 'base64'));
    if (str.startsWith('raw:')) return Buffer.from(str.slice(4), 'base64').toString('utf8');
    return str;
  },
};

function clientId() {
  return (store.settings.azureClientId || config.azureClientId || '').trim();
}

function send(channel, data) {
  if (win && !win.isDestroyed()) win.webContents.send(channel, data);
}

function createWindow() {
  win = new BrowserWindow({
    width: 1100,
    height: 680,
    minWidth: 940,
    minHeight: 600,
    frame: false,
    show: false,
    backgroundColor: '#05080d',
    title: 'Rainy Launcher',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      spellcheck: false,
    },
  });
  win.setMenu(null);
  win.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  win.once('ready-to-show', () => win.show());
  // Odkazy z launcheru otevírat v prohlížeči, ne v okně
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('https://')) shell.openExternal(url);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (e) => e.preventDefault());
}

app.whenReady().then(async () => {
  store = new Store(path.join(app.getPath('userData')), crypto);
  await store.load();
  createWindow();
});

app.on('window-all-closed', () => {
  // Když běží hra, launcher se jen schová
  if (!game) app.quit();
});

// ------------------------------------------------------------------ IPC

const ok = (data) => ({ ok: true, ...data });
const fail = (e) => ({ ok: false, error: e?.message || String(e), code: e?.code });

ipcMain.handle('init', async () => ok({
  version: app.getVersion(),
  mc: config.minecraft,
  loader: config.fabricLoader,
  settings: store.settings,
  account: store.publicAccount(),
  loginReady: Boolean(clientId()),
  totalRamGb: Math.floor(os.totalmem() / 1024 ** 3),
  mods: config.mods.filter((m) => !m.library).map((m) => ({
    id: m.id, name: m.name, description: m.description, required: Boolean(m.required), group: m.group || 'core', needs: m.needs || [],
  })),
  running: Boolean(game),
}));

ipcMain.handle('settings:save', async (_e, patch) => {
  const allowedKeys = ['ramGb', 'hideOnLaunch', 'azureClientId', 'mods'];
  const clean = Object.fromEntries(Object.entries(patch || {}).filter(([k]) => allowedKeys.includes(k)));
  const settings = await store.saveSettings(clean);
  return ok({ settings, loginReady: Boolean(clientId()) });
});

ipcMain.on('win:minimize', () => win?.minimize());
ipcMain.on('win:close', () => win?.close());

ipcMain.handle('open', async (_e, what) => {
  const target = { game: paths.game, mods: path.join(paths.game, 'mods'), logs: paths.logs }[what];
  if (!target) return fail(new Error('Neznámá složka'));
  fs.mkdirSync(target, { recursive: true });
  await shell.openPath(target);
  return ok();
});

ipcMain.handle('open:url', async (_e, url) => {
  if (typeof url === 'string' && url.startsWith('https://')) await shell.openExternal(url);
  return ok();
});

// ------------------------------------------------------------------ přihlášení

function loginWindow(id) {
  const pkce = auth.createPkce();
  const popup = new BrowserWindow({
    width: 480,
    height: 640,
    parent: win,
    modal: true,
    autoHideMenuBar: true,
    title: 'Přihlášení Microsoft',
    backgroundColor: '#ffffff',
    webPreferences: { partition: `mslogin-${Date.now()}`, contextIsolation: true, nodeIntegration: false },
  });
  popup.setMenu(null);
  return new Promise((resolve, reject) => {
    let finished = false;
    const check = (url) => {
      if (!url || !url.startsWith(auth.REDIRECT)) return false;
      finished = true;
      const u = new URL(url);
      const code = u.searchParams.get('code');
      const error = u.searchParams.get('error');
      setImmediate(() => popup.destroy());
      if (code && u.searchParams.get('state') === pkce.state) {
        resolve(code);
      } else {
        const msg = error === 'access_denied' ? 'Přihlášení zrušeno.' : `Přihlášení selhalo: ${u.searchParams.get('error_description') || error}`;
        reject(new auth.AuthError('CANCEL', msg));
      }
      return true;
    };
    popup.webContents.on('will-redirect', (e, url) => { if (check(e.url || url)) e.preventDefault(); });
    popup.webContents.on('will-navigate', (e, url) => { if (check(e.url || url)) e.preventDefault(); });
    popup.webContents.on('did-navigate', (_e, url) => check(url));
    popup.on('closed', () => {
      if (!finished) reject(new auth.AuthError('CANCEL', 'Přihlášení zrušeno.'));
    });
    popup.loadURL(auth.authorizeUrl(id, pkce));
  }).then((code) => auth.loginWithCode(id, code, pkce));
}

ipcMain.handle('login', async () => {
  try {
    const id = clientId();
    if (!id) throw new auth.AuthError('NO_CLIENT', 'Přihlášení ještě není zapnuté (chybí schválená aplikace).');
    const account = await loginWindow(id);
    await store.saveAccount(account);
    return ok({ account: store.publicAccount() });
  } catch (e) {
    return fail(e);
  }
});

ipcMain.handle('logout', async () => {
  await store.saveAccount(null);
  return ok();
});

// ------------------------------------------------------------------ hraní

// Čísla pro progress bar (text etapy posílá stage())
const progress = (p) => send('progress', { done: p.done, total: p.total, bytes: p.bytes, totalBytes: p.totalBytes });
const stage = (text) => send('progress', { stage: text });

function iconDataUrl() {
  try {
    return 'data:image/png;base64,' + fs.readFileSync(path.join(__dirname, 'assets', 'profile-icon.png')).toString('base64');
  } catch {
    return null;
  }
}

async function playDirect(id) {
  stage('Kontroluju účet');
  const account = await auth.ensureFresh(id, store.account);
  await store.saveAccount(account);

  const profile = await minecraft.prepare(config.minecraft, config.fabricLoader, {
    onStage: stage,
    onProgress: progress,
  });
  stage('Stahuju Javu');
  const javaExe = await java.ensureJava(profile.javaComponent, progress);
  await mods.install(config, store.settings, { onStage: stage, onProgress: progress });

  stage('Spouštím hru');
  // Herní grafika (u notebooků velký rozdíl). Musí být nastavená před startem hry.
  await perf.preferHighPerformanceGpu([javaExe, javaExe.replace(/javaw\.exe$/i, 'java.exe')]);

  const child = await launcher.launch(profile, account, {
    java: javaExe,
    ramGb: store.settings.ramGb,
    launcherVersion: app.getVersion(),
  });
  game = child;
  perf.boostPriority(child.pid);
  const started = Date.now();
  send('game', { state: 'running' });
  if (store.settings.hideOnLaunch) setTimeout(() => game && win?.hide(), 4000);

  child.on('exit', (code) => {
    game = null;
    const crashed = code !== 0 && code !== null;
    if (win && !win.isDestroyed()) {
      win.show();
      send('game', { state: 'stopped', crashed, code, seconds: Math.round((Date.now() - started) / 1000) });
    } else {
      app.quit();
    }
  });
  child.on('error', (e) => {
    game = null;
    win?.show();
    send('game', { state: 'stopped', crashed: true, error: e.message });
  });
  return { mode: 'direct' };
}

async function playOfficial() {
  stage('Připravuju Fabric');
  const fabricJson = await minecraft.loadFabric(config.minecraft, config.fabricLoader);
  await mods.install(config, store.settings, { onStage: stage, onProgress: progress });

  stage('Vytvářím profil Rainy Client');
  const ram = Math.max(2, Math.round(store.settings.ramGb || 4));
  await official.setupProfile({
    versionId: fabricJson.id,
    fabricJson,
    javaArgs: `-Xms2G -Xmx${ram}G -XX:+UseZGC -XX:+UseCompactObjectHeaders -XX:+UseStringDeduplication`,
    iconDataUrl: iconDataUrl(),
  });
  stage('Otevírám Minecraft Launcher');
  await perf.preferHighPerformanceGpu(perf.officialJavaPaths());
  const opened = official.openOfficialLauncher();
  return { mode: 'official', opened };
}

ipcMain.handle('play', async () => {
  if (busy || game) return fail(new Error('Hra už se spouští.'));
  busy = true;
  try {
    if (!store.settings.imported) {
      await official.importFromMinecraft().catch(() => []);
      await store.saveSettings({ imported: true });
    }
    const id = clientId();
    const result = id && store.account ? await playDirect(id) : await playOfficial();
    return ok(result);
  } catch (e) {
    console.error(e);
    return fail(e);
  } finally {
    busy = false;
    send('progress', null);
  }
});
