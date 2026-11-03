'use strict';
// Kde všechno leží na disku.
// Windows: %APPDATA%\.rainyclient (stejně jako .minecraft, jen vlastní složka)

const os = require('os');
const path = require('path');

const APPDATA = process.env.APPDATA
  || (process.platform === 'darwin'
    ? path.join(os.homedir(), 'Library', 'Application Support')
    : path.join(os.homedir(), '.local', 'share'));

const ROOT = process.env.RAINY_ROOT || path.join(APPDATA, '.rainyclient');

module.exports = {
  APPDATA,
  ROOT,
  runtime: path.join(ROOT, 'runtime'),
  libraries: path.join(ROOT, 'libraries'),
  assets: path.join(ROOT, 'assets'),
  versions: path.join(ROOT, 'versions'),
  natives: path.join(ROOT, 'natives'),
  cache: path.join(ROOT, 'cache'),
  logs: path.join(ROOT, 'logs'),
  // Složka samotné hry: mody, světy, options.txt, screenshoty
  game: path.join(ROOT, 'game'),
  // Normální Minecraft launcher
  officialMinecraft: path.join(APPDATA, '.minecraft'),
};
