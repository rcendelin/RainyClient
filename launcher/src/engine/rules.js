'use strict';
// Pravidla z Mojang JSONu ("rules"): která knihovna/argument platí pro tenhle systém.

const os = require('os');

function osName() {
  return { win32: 'windows', darwin: 'osx', linux: 'linux' }[process.platform] || process.platform;
}

function osArch() {
  return { x64: 'x86_64', ia32: 'x86', arm64: 'arm64' }[process.arch] || process.arch;
}

function compareVersions(a, b) {
  const pa = String(a).split('.').map((n) => parseInt(n, 10) || 0);
  const pb = String(b).split('.').map((n) => parseInt(n, 10) || 0);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const d = (pa[i] || 0) - (pb[i] || 0);
    if (d) return d;
  }
  return 0;
}

function osMatches(rule) {
  if (!rule.os) return true;
  const o = rule.os;
  if (o.name && o.name !== osName()) return false;
  if (o.arch && o.arch !== osArch()) return false;
  const release = os.release();
  if (o.version) {
    try {
      if (!new RegExp(o.version).test(release)) return false;
    } catch {
      return false;
    }
  }
  if (o.versionRange) {
    if (o.versionRange.min && compareVersions(release, o.versionRange.min) < 0) return false;
    if (o.versionRange.max && compareVersions(release, o.versionRange.max) >= 0) return false;
  }
  return true;
}

function featuresMatch(rule, features) {
  if (!rule.features) return true;
  for (const [k, v] of Object.entries(rule.features)) {
    if (Boolean(features[k]) !== Boolean(v)) return false;
  }
  return true;
}

/** Vyhodnotí pole pravidel. Bez pravidel = povoleno. */
function allowed(rules, features = {}) {
  if (!rules || rules.length === 0) return true;
  let result = false;
  for (const rule of rules) {
    if (osMatches(rule) && featuresMatch(rule, features)) {
      result = rule.action === 'allow';
    }
  }
  return result;
}

/** "group:artifact:version[:classifier][@ext]" -> cesta v maven repozitáři */
function mavenPath(name) {
  let ext = 'jar';
  let coords = name;
  const at = name.indexOf('@');
  if (at >= 0) {
    ext = name.slice(at + 1);
    coords = name.slice(0, at);
  }
  const [group, artifact, version, classifier] = coords.split(':');
  const file = `${artifact}-${version}${classifier ? '-' + classifier : ''}.${ext}`;
  return `${group.replace(/\./g, '/')}/${artifact}/${version}/${file}`;
}

/** Klíč pro odstranění duplicit: group:artifact[:classifier] (bez verze) */
function libraryKey(name) {
  const [group, artifact, , classifier] = name.split('@')[0].split(':');
  return `${group}:${artifact}${classifier ? ':' + classifier : ''}`;
}

module.exports = { allowed, mavenPath, libraryKey, osName, osArch, compareVersions };
