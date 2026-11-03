'use strict';
/* global rainy */

const $ = (s) => document.querySelector(s);
const $$ = (s) => [...document.querySelectorAll(s)];

const state = {
  settings: {},
  mods: [],
  account: null,
  loginReady: false,
  running: false,
  busy: false,
  totalRamGb: 8,
};

function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// ------------------------------------------------------------ hlášky

function toast(title, text, { error = false, actions = [], timeout = 6000 } = {}) {
  const el = document.createElement('div');
  el.className = 'toast' + (error ? ' error' : '');
  el.innerHTML = `<b>${esc(title)}</b>${text ? `<p>${esc(text)}</p>` : ''}`;
  if (actions.length) {
    const row = document.createElement('div');
    row.className = 'toast-actions';
    for (const a of actions) {
      const b = document.createElement('button');
      b.className = 'btn';
      b.textContent = a.label;
      b.onclick = () => { a.onClick(); close(); };
      row.appendChild(b);
    }
    el.appendChild(row);
  }
  const close = () => {
    el.classList.add('out');
    setTimeout(() => el.remove(), 200);
  };
  el.addEventListener('click', (e) => { if (e.target === el) close(); });
  $('#toasts').appendChild(el);
  if (timeout) setTimeout(close, timeout);
}

// ------------------------------------------------------------ navigace

$$('.nav-item').forEach((btn) => {
  btn.addEventListener('click', () => {
    $$('.nav-item').forEach((b) => b.classList.toggle('active', b === btn));
    $$('.page').forEach((p) => p.classList.toggle('active', p.id === `page-${btn.dataset.page}`));
  });
});

$('#btn-min').onclick = () => rainy.minimize();
$('#btn-close').onclick = () => rainy.close();

// ------------------------------------------------------------ účet

function renderAccount() {
  const box = $('#account');
  if (state.account) {
    const a = state.account;
    box.innerHTML = `
      <div class="account-card">
        <img src="https://mc-heads.net/avatar/${esc(a.uuid)}/68" alt="">
        <div class="account-meta">
          <div class="account-name">${esc(a.name)}</div>
          <button class="link-btn" id="btn-logout">Odhlásit</button>
        </div>
      </div>`;
    const img = box.querySelector('img');
    img.onerror = () => img.replaceWith(Object.assign(document.createElement('div'), { className: 'avatar-empty' }));
    $('#btn-logout').onclick = async () => {
      await rainy.logout();
      state.account = null;
      renderAccount();
      renderPlayInfo();
    };
  } else {
    box.innerHTML = `
      <button class="login-btn" id="btn-login">
        <span class="ms-logo"><i></i><i></i><i></i><i></i></span>
        Přihlásit se
      </button>`;
    $('#btn-login').onclick = login;
  }
}

async function login() {
  if (!state.loginReady) {
    toast('Přihlášení zatím nejde',
      'Zapne se, až Microsoft schválí Rainy Launcher. Do té doby klikni na HRÁT a hra se spustí přes Minecraft Launcher.',
      { timeout: 9000 });
    return;
  }
  const r = await rainy.login();
  if (r.ok) {
    state.account = r.account;
    renderAccount();
    renderPlayInfo();
    toast(`Ahoj ${r.account.name}!`, 'Jsi přihlášený, můžeš hrát.');
  } else if (r.code !== 'CANCEL') {
    toast('Přihlášení se nepovedlo', r.error, { error: true, timeout: 10000 });
  }
}

// ------------------------------------------------------------ hrát

function enabledModCount() {
  const on = new Set(state.mods.filter((m) => m.required || state.settings.mods?.[m.id] !== false).map((m) => m.id));
  for (const m of state.mods) {
    if (on.has(m.id) && m.needs.some((n) => !on.has(n))) on.delete(m.id);
  }
  return on.size;
}

function renderPlayInfo() {
  const n = enabledModCount();
  $('#mods-count').textContent = `${n} ${n >= 5 ? 'modů' : n >= 2 ? 'mody' : 'mod'} zapnuto · ${state.settings.ramGb} GB RAM`;
  const direct = state.loginReady && state.account;
  $('#mode-hint').textContent = direct
    ? `Spustí se přímo jako ${state.account.name}.`
    : 'Spustí se přes Minecraft Launcher, tam vyber profil Rainy Client.';
}

function setPlayButton() {
  const btn = $('#btn-play');
  btn.classList.toggle('running', state.running);
  btn.disabled = state.busy || state.running;
  $('#play-label').textContent = state.running ? 'HRA BĚŽÍ' : state.busy ? 'CHVILKU' : 'HRÁT';
}

function fmtMb(b) {
  return (b / 1024 / 1024).toFixed(b > 100 * 1024 * 1024 ? 0 : 1);
}

rainy.onProgress((p) => {
  const box = $('#progress');
  const fill = $('#bar-fill');
  if (!p) {
    box.hidden = true;
    return;
  }
  box.hidden = false;
  if (p.stage) {
    $('#progress-stage').textContent = p.stage;
    $('#progress-num').textContent = '';
    fill.classList.add('indeterminate');
    return;
  }
  if (!p.total) return;
  fill.classList.remove('indeterminate');
  const pct = p.totalBytes ? (p.bytes / p.totalBytes) : (p.done / p.total);
  fill.style.width = `${Math.max(2, Math.min(100, pct * 100)).toFixed(1)}%`;
  $('#progress-num').textContent = p.totalBytes
    ? `${fmtMb(p.bytes)} / ${fmtMb(p.totalBytes)} MB`
    : `${p.done} / ${p.total}`;
});

rainy.onGame((g) => {
  state.running = g.state === 'running';
  setPlayButton();
  if (g.state === 'stopped' && g.crashed) {
    toast('Hra spadla', g.error || `Minecraft skončil s chybou (kód ${g.code}). Pošli mi log, mrknu na to.`, {
      error: true,
      timeout: 0,
      actions: [{ label: 'Otevřít logy', onClick: () => rainy.open('logs') }],
    });
  }
});

$('#btn-play').onclick = async () => {
  if (state.busy || state.running) return;
  state.busy = true;
  setPlayButton();
  const r = await rainy.play();
  state.busy = false;
  setPlayButton();
  if (!r.ok) {
    toast('Nepodařilo se spustit hru', r.error, { error: true, timeout: 12000 });
    return;
  }
  if (r.mode === 'official') {
    if (r.opened) {
      toast('Skoro hotovo', 'V Minecraft Launcheru vyber vlevo dole profil "Rainy Client" a klikni na Hrát.', { timeout: 14000 });
    } else {
      toast('Otevři Minecraft Launcher', 'Profil "Rainy Client" je připravený. Otevři normální Minecraft Launcher, vyber ho a dej Hrát.', { timeout: 0 });
    }
  }
};

// ------------------------------------------------------------ mody

const GROUPS = [
  { id: 'core', title: 'Základ' },
  { id: 'fps', title: 'FPS a výkon' },
  { id: 'extra', title: 'Extra' },
];

function modOn(m) {
  return m.required || state.settings.mods?.[m.id] !== false;
}

function renderMods() {
  const root = $('#mod-groups');
  root.innerHTML = '';
  for (const g of GROUPS) {
    const list = state.mods.filter((m) => m.group === g.id);
    if (!list.length) continue;
    const section = document.createElement('div');
    section.innerHTML = `<div class="group-title">${esc(g.title)}</div><div class="mod-list"></div>`;
    const grid = section.querySelector('.mod-list');
    for (const m of list) {
      const missing = m.needs.filter((n) => !modOn(state.mods.find((x) => x.id === n) || { required: true }));
      const on = modOn(m) && missing.length === 0;
      const el = document.createElement('label');
      el.className = 'mod' + (on ? ' on' : '');
      el.innerHTML = `
        <div class="mod-icon ${m.id === 'rainy-client' ? 'rainy' : ''}">${m.id === 'rainy-client' ? '' : esc(m.name[0])}</div>
        <div class="mod-text">
          <div class="mod-name">${esc(m.name)}${m.required ? '<span class="tag">vždy</span>' : ''}</div>
          <div class="mod-desc">${esc(missing.length ? `Potřebuje ${missing.join(', ')}.` : m.description)}</div>
        </div>
        <input type="checkbox" class="switch" ${on ? 'checked' : ''} ${m.required || missing.length ? 'disabled' : ''}>`;
      el.querySelector('input').addEventListener('change', async (e) => {
        const mods = { ...(state.settings.mods || {}), [m.id]: e.target.checked };
        const r = await rainy.saveSettings({ mods });
        if (r.ok) state.settings = r.settings;
        renderMods();
        renderPlayInfo();
      });
      grid.appendChild(el);
    }
    root.appendChild(section);
  }
}

// ------------------------------------------------------------ nastavení

function renderSettings() {
  const max = Math.max(2, Math.min(16, state.totalRamGb - 2));
  const ram = $('#ram');
  ram.max = max;
  ram.value = Math.min(state.settings.ramGb, max);
  $('#ram-value').textContent = ram.value;
  $('#ram-hint').textContent = `Tvůj PC má ${state.totalRamGb} GB. Na PvP stačí 4 GB, víc než polovinu nedávej.`;
  $('#hide-on-launch').checked = Boolean(state.settings.hideOnLaunch);
  $('#client-id').value = state.settings.azureClientId || '';
  $('#client-hint').textContent = state.loginReady
    ? 'Přihlášení je zapnuté. Přihlas se vlevo dole.'
    : 'Dokud tu nic není, hra se spouští přes normální Minecraft Launcher.';
}

let ramTimer;
$('#ram').addEventListener('input', (e) => {
  $('#ram-value').textContent = e.target.value;
  clearTimeout(ramTimer);
  ramTimer = setTimeout(async () => {
    const r = await rainy.saveSettings({ ramGb: Number(e.target.value) });
    if (r.ok) state.settings = r.settings;
    renderPlayInfo();
  }, 250);
});

$('#hide-on-launch').addEventListener('change', async (e) => {
  const r = await rainy.saveSettings({ hideOnLaunch: e.target.checked });
  if (r.ok) state.settings = r.settings;
});

$('#client-id').addEventListener('change', async (e) => {
  const value = e.target.value.trim();
  if (value && !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)) {
    toast('Tohle nevypadá jako Client ID', 'Má tvar xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.', { error: true });
    return;
  }
  const r = await rainy.saveSettings({ azureClientId: value });
  if (r.ok) {
    state.settings = r.settings;
    state.loginReady = r.loginReady;
    renderSettings();
    renderPlayInfo();
    if (value) toast('Uloženo', 'Teď se můžeš přihlásit přes Microsoft vlevo dole.');
  }
});

$$('[data-open]').forEach((b) => {
  b.onclick = () => rainy.open(b.dataset.open);
});

// ------------------------------------------------------------ déšť v pozadí

(function rain() {
  const canvas = $('#rain');
  const ctx = canvas.getContext('2d');
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  let drops = [];
  let w = 0;
  let h = 0;
  function resize() {
    const dpr = window.devicePixelRatio || 1;
    w = canvas.clientWidth;
    h = canvas.clientHeight;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    const count = Math.round((w * h) / 9000);
    drops = Array.from({ length: count }, () => spawn(true));
  }
  function spawn(anywhere) {
    const z = Math.random();
    return {
      x: Math.random() * (w + 200) - 100,
      y: anywhere ? Math.random() * h : -40 - Math.random() * 100,
      len: 10 + z * 22,
      speed: 4 + z * 9,
      alpha: 0.05 + z * 0.16,
    };
  }
  function frame() {
    ctx.clearRect(0, 0, w, h);
    ctx.lineWidth = 1;
    for (const d of drops) {
      ctx.strokeStyle = `rgba(79,195,247,${d.alpha})`;
      ctx.beginPath();
      ctx.moveTo(d.x, d.y);
      ctx.lineTo(d.x - d.len * 0.18, d.y + d.len);
      ctx.stroke();
      d.y += d.speed;
      d.x -= d.speed * 0.18;
      if (d.y > h + 20) Object.assign(d, spawn(false));
    }
    if (!reduce) requestAnimationFrame(frame);
  }
  window.addEventListener('resize', resize);
  resize();
  frame();
})();

// ------------------------------------------------------------ start

(async () => {
  const r = await rainy.init();
  Object.assign(state, {
    settings: r.settings,
    mods: r.mods,
    account: r.account,
    loginReady: r.loginReady,
    running: r.running,
    totalRamGb: r.totalRamGb,
  });
  $('#eyebrow').textContent = `Minecraft ${r.mc} · Fabric ${r.loader}`;
  $('#version-line').textContent = `Rainy Launcher ${r.version} · Minecraft ${r.mc} · Fabric ${r.loader}`;
  renderAccount();
  renderMods();
  renderSettings();
  renderPlayInfo();
  setPlayButton();
})();
