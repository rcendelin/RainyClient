'use strict';
// Nastavení launcheru a uložený účet (refresh token je zašifrovaný přes Windows).

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');

const DEFAULT_SETTINGS = {
  ramGb: 4,
  hideOnLaunch: true,
  azureClientId: '',
  mods: {},
  imported: false,
};

class Store {
  /** dir = složka pro data launcheru, crypto = { encrypt(str)->str, decrypt(str)->str } */
  constructor(dir, crypto) {
    this.dir = dir;
    this.crypto = crypto;
    this.settingsFile = path.join(dir, 'settings.json');
    this.accountFile = path.join(dir, 'account.json');
    this.settings = { ...DEFAULT_SETTINGS };
    this.account = null;
  }

  async load() {
    await fsp.mkdir(this.dir, { recursive: true });
    try {
      this.settings = { ...DEFAULT_SETTINGS, ...JSON.parse(await fsp.readFile(this.settingsFile, 'utf8')) };
    } catch {}
    try {
      const raw = JSON.parse(await fsp.readFile(this.accountFile, 'utf8'));
      this.account = { ...raw, refreshToken: this.crypto.decrypt(raw.refreshToken), accessToken: this.crypto.decrypt(raw.accessToken) };
    } catch {
      this.account = null;
    }
  }

  async saveSettings(patch) {
    this.settings = { ...this.settings, ...patch };
    await fsp.writeFile(this.settingsFile, JSON.stringify(this.settings, null, 2));
    return this.settings;
  }

  async saveAccount(account) {
    this.account = account;
    if (!account) {
      await fsp.rm(this.accountFile, { force: true });
      return;
    }
    const raw = {
      ...account,
      refreshToken: this.crypto.encrypt(account.refreshToken || ''),
      accessToken: this.crypto.encrypt(account.accessToken || ''),
    };
    await fsp.writeFile(this.accountFile, JSON.stringify(raw, null, 2));
  }

  /** Účet bez tokenů, pro UI */
  publicAccount() {
    if (!this.account) return null;
    return { name: this.account.name, uuid: this.account.uuid };
  }
}

module.exports = { Store, DEFAULT_SETTINGS };
