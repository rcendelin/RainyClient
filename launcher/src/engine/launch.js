'use strict';
// Sestaví příkaz pro Javu a spustí hru.

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const { spawn } = require('child_process');
const paths = require('./paths');
const { allowed } = require('./rules');

function substitute(str, vars) {
  return str.replace(/\$\{([^}]+)\}/g, (m, key) => (key in vars ? String(vars[key]) : m));
}

function collect(list, features, vars) {
  const out = [];
  for (const arg of list || []) {
    if (typeof arg === 'string') {
      out.push(substitute(arg, vars));
    } else if (arg && allowed(arg.rules, features)) {
      const values = Array.isArray(arg.value) ? arg.value : [arg.value];
      for (const v of values) out.push(substitute(v, vars));
    }
  }
  return out;
}

/**
 * profile = výsledek minecraft.prepare(), account = { name, uuid, accessToken, xuid }
 * options = { java, ramGb, launcherVersion, server? }
 */
function buildArgs(profile, account, options) {
  const nativesDir = path.join(paths.natives, profile.id);
  const vars = {
    auth_player_name: account.name,
    auth_uuid: account.uuid.replace(/-/g, ''),
    auth_access_token: account.accessToken,
    auth_xuid: account.xuid || '0',
    auth_session: account.accessToken,
    clientid: account.clientId || '',
    user_type: 'msa',
    user_properties: '{}',
    version_name: profile.id,
    version_type: profile.type,
    game_directory: paths.game,
    assets_root: paths.assets,
    game_assets: paths.assets,
    assets_index_name: profile.assetIndex,
    natives_directory: nativesDir,
    library_directory: paths.libraries,
    classpath_separator: path.delimiter,
    launcher_name: 'rainy-launcher',
    launcher_version: options.launcherVersion || '1.0.0',
    classpath: profile.classpath.join(path.delimiter),
    quickPlayMultiplayer: options.server || '',
  };
  const features = {
    is_demo_user: false,
    has_custom_resolution: false,
    is_quick_play_multiplayer: Boolean(options.server),
  };

  const ram = Math.max(2, Math.round(options.ramGb || 4));
  // Mojang doporučené JVM argumenty (GC atd.), jen RAM nastavíme podle hráče
  const userJvm = collect(profile.arguments['default-user-jvm'], features, vars)
    .filter((a) => !/^-Xm[sx]/.test(a));
  const jvm = [
    `-Xms${Math.min(2, ram)}G`,
    `-Xmx${ram}G`,
    ...userJvm,
    ...collect(profile.arguments.jvm, features, vars),
  ];
  if (profile.logging) jvm.push(profile.logging);
  const game = collect(profile.arguments.game, features, vars);
  return { args: [...jvm, profile.mainClass, ...game], nativesDir };
}

/** Spustí hru. Vrátí child process; výstup jde do logs/game-output.log */
async function launch(profile, account, options) {
  const { args, nativesDir } = buildArgs(profile, account, options);
  await fsp.mkdir(nativesDir, { recursive: true });
  await fsp.mkdir(paths.game, { recursive: true });
  await fsp.mkdir(paths.logs, { recursive: true });

  const logFile = path.join(paths.logs, 'game-output.log');
  const log = fs.createWriteStream(logFile);
  // Do logu nikdy nedávat token
  const safe = args.map((a) => (a === account.accessToken ? '***' : a));
  log.write(`> ${options.java}\n> ${safe.join(' ')}\n\n`);

  const child = spawn(options.java, args, {
    cwd: paths.game,
    windowsHide: false,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  child.stdout.pipe(log, { end: false });
  child.stderr.pipe(log, { end: false });
  child.on('exit', () => log.end());
  child.logFile = logFile;
  return child;
}

module.exports = { launch, buildArgs };
