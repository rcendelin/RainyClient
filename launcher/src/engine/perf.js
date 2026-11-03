'use strict';
// Výkon na Windows: hra na herní (výkonné) grafice a s vyšší prioritou.

const os = require('os');
const path = require('path');
const { execFile } = require('child_process');
const paths = require('./paths');

const GPU_KEY = 'HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences';

function reg(args) {
  return new Promise((resolve) => {
    execFile('reg', args, { windowsHide: true }, (err) => resolve(!err));
  });
}

/**
 * Řekne Windows, ať tyhle programy pouští na výkonné grafice ("GpuPreference=2").
 * Stejné jako Nastavení > Displej > Grafika > Vysoký výkon. Na PC s jednou grafikou nic nemění.
 */
async function preferHighPerformanceGpu(exePaths) {
  if (process.platform !== 'win32') return 0;
  let ok = 0;
  for (const exe of new Set(exePaths.filter(Boolean))) {
    if (await reg(['add', GPU_KEY, '/v', exe, '/t', 'REG_SZ', '/d', 'GpuPreference=2;', '/f'])) ok++;
  }
  return ok;
}

/** Java, kterou používá normální Minecraft Launcher (kvůli záložnímu režimu). */
function officialJavaPaths(component = 'java-runtime-epsilon') {
  if (process.platform !== 'win32') return [];
  const rel = ['runtime', component, 'windows-x64', component, 'bin'];
  const roots = [
    paths.officialMinecraft,
    path.join(process.env.LOCALAPPDATA || '', 'Packages', 'Microsoft.4297127D64EC6_8wekyb3d8bbwe', 'LocalCache', 'Local'),
  ];
  const out = [];
  for (const root of roots) {
    out.push(path.join(root, ...rel, 'javaw.exe'), path.join(root, ...rel, 'java.exe'));
  }
  return out;
}

/** Vyšší priorita procesu hry (o stupeň nad normál, ať se PC nezasekne). */
function boostPriority(pid) {
  try {
    os.setPriority(pid, os.constants.priority.PRIORITY_ABOVE_NORMAL);
    return true;
  } catch {
    return false;
  }
}

module.exports = { preferHighPerformanceGpu, officialJavaPaths, boostPriority };
