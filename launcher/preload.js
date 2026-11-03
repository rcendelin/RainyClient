'use strict';
// Most mezi oknem launcheru a hlavním procesem. Okno nemá přístup k Node, jen k těmhle funkcím.

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('rainy', {
  init: () => ipcRenderer.invoke('init'),
  saveSettings: (patch) => ipcRenderer.invoke('settings:save', patch),
  login: () => ipcRenderer.invoke('login'),
  logout: () => ipcRenderer.invoke('logout'),
  play: () => ipcRenderer.invoke('play'),
  open: (what) => ipcRenderer.invoke('open', what),
  openUrl: (url) => ipcRenderer.invoke('open:url', url),
  minimize: () => ipcRenderer.send('win:minimize'),
  close: () => ipcRenderer.send('win:close'),
  onProgress: (fn) => ipcRenderer.on('progress', (_e, p) => fn(p)),
  onGame: (fn) => ipcRenderer.on('game', (_e, g) => fn(g)),
});
