'use strict';
// Sestaví Windows verzi: aplikace (Electron) -> ikonka -> instalátor (NSIS).
// Spuštění: npm run pack:win   (funguje i na Linuxu, bez Wine)

const path = require('path');
const fs = require('fs');
const { execFileSync } = require('child_process');
const { packager } = require('@electron/packager');
const ResEdit = require('resedit');

const ROOT = path.join(__dirname, '..');
const pkg = require('../package.json');
const KEEP = ['/main.js', '/preload.js', '/config.json', '/package.json', '/src', '/renderer', '/assets'];

function setExeIcon(exe) {
  const data = fs.readFileSync(exe);
  const pe = ResEdit.NtExecutable.from(data, { ignoreCert: true });
  const res = ResEdit.NtExecutableResource.from(pe);
  const iconFile = ResEdit.Data.IconFile.from(fs.readFileSync(path.join(ROOT, 'assets', 'icon.ico')));
  ResEdit.Resource.IconGroupEntry.replaceIconsForResource(
    res.entries, 1, 1033, iconFile.icons.map((i) => i.data),
  );
  const vi = ResEdit.Resource.VersionInfo.fromEntries(res.entries)[0];
  const [a, b, c] = pkg.version.split('.').map(Number);
  vi.setFileVersion(a, b, c, 0, 1033);
  vi.setProductVersion(a, b, c, 0, 1033);
  vi.setStringValues({ lang: 1033, codepage: 1200 }, {
    FileDescription: 'Rainy Launcher',
    ProductName: 'Rainy Launcher',
    CompanyName: 'NEBIXY',
    LegalCopyright: 'NEBIXY',
    OriginalFilename: 'RainyLauncher.exe',
    InternalName: 'RainyLauncher',
  });
  vi.outputToResourceEntries(res.entries);
  res.outputResource(pe);
  fs.writeFileSync(exe, Buffer.from(pe.generate()));
}

(async () => {
  // Kontrola: Rainy jar musí mít fabric.mod.json, jinak ho Fabric nenačte
  const jar = path.join(ROOT, 'bundled', 'rainy-client.jar');
  const list = execFileSync('unzip', ['-Z1', jar]).toString().split('\n');
  for (const need of ['fabric.mod.json', 'rainy-client.client.mixins.json']) {
    if (!list.includes(need)) throw new Error(`bundled/rainy-client.jar nemá ${need}`);
  }

  const out = path.join(ROOT, 'dist');
  const [appDir] = await packager({
    dir: ROOT,
    out,
    name: 'Rainy Launcher',
    executableName: 'RainyLauncher',
    platform: 'win32',
    arch: 'x64',
    appVersion: pkg.version,
    asar: true,
    overwrite: true,
    prune: true,
    extraResource: [path.join(ROOT, 'bundled')],
    // Předem stažený Electron (složka s electron-vX-win32-x64.zip), když je nastavená
    ...(process.env.ELECTRON_ZIP_DIR ? { electronZipDir: process.env.ELECTRON_ZIP_DIR } : {}),
    ignore: (p) => p !== '' && !KEEP.some((k) => p === k || p.startsWith(k + '/')),
  });
  console.log('App:', appDir);

  // Jazyky Chromia: necháme jen češtinu, slovenštinu a angličtinu (menší instalátor)
  const locales = path.join(appDir, 'locales');
  for (const f of fs.readdirSync(locales)) {
    if (!['cs.pak', 'sk.pak', 'en-US.pak'].includes(f)) fs.rmSync(path.join(locales, f));
  }

  const exe = path.join(appDir, 'RainyLauncher.exe');
  setExeIcon(exe);
  console.log('Ikonka nastavena');

  const setup = path.join(out, 'RainyLauncherSetup.exe');
  execFileSync('makensis', [
    '-V2',
    `-DSRC=${appDir}`,
    `-DOUT=${setup}`,
    `-DICON=${path.join(ROOT, 'assets', 'icon.ico')}`,
    `-DVERSION=${pkg.version}`,
    path.join(__dirname, 'installer.nsi'),
  ], { stdio: 'inherit' });
  console.log('Instalátor:', setup, (fs.statSync(setup).size / 1024 / 1024).toFixed(1), 'MB');
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
