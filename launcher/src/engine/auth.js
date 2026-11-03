'use strict';
// Přihlášení Microsoft -> Xbox -> Minecraft.
// Potřebuje Azure aplikaci (client ID), kterou Microsoft schválil pro Minecraft.

const crypto = require('crypto');
const net = require('./net');

const AUTHORIZE = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize';
const TOKEN = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/token';
const REDIRECT = 'https://login.microsoftonline.com/common/oauth2/nativeclient';
const SCOPE = 'XboxLive.signin offline_access';

class AuthError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}

function base64url(buf) {
  return buf.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function createPkce() {
  const verifier = base64url(crypto.randomBytes(32));
  const challenge = base64url(crypto.createHash('sha256').update(verifier).digest());
  const state = base64url(crypto.randomBytes(12));
  return { verifier, challenge, state };
}

function authorizeUrl(clientId, pkce) {
  const q = new URLSearchParams({
    client_id: clientId,
    response_type: 'code',
    redirect_uri: REDIRECT,
    scope: SCOPE,
    code_challenge: pkce.challenge,
    code_challenge_method: 'S256',
    state: pkce.state,
    prompt: 'select_account',
  });
  return `${AUTHORIZE}?${q}`;
}

async function msToken(clientId, params) {
  const res = await net.fetchWithRetry(TOKEN, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ client_id: clientId, scope: SCOPE, redirect_uri: REDIRECT, ...params }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok || !data.access_token) {
    const desc = data.error_description || data.error || `HTTP ${res.status}`;
    if (data.error === 'invalid_grant') throw new AuthError('EXPIRED', 'Přihlášení vypršelo, přihlas se znovu.');
    throw new AuthError('MS', `Microsoft přihlášení selhalo: ${desc.split('\r\n')[0]}`);
  }
  return data;
}

const XERR = {
  2148916233: 'Tenhle účet nemá Xbox profil. Přihlas se jednou na minecraft.net a zkus to znovu.',
  2148916235: 'Xbox Live v téhle zemi nefunguje.',
  2148916236: 'Účet potřebuje ověření věku na xbox.com.',
  2148916237: 'Účet potřebuje ověření věku na xbox.com.',
  2148916238: 'Dětský účet musí být v Microsoft rodině (family.microsoft.com), požádej rodiče.',
};

async function postJson(url, body, headers = {}) {
  const res = await net.fetchWithRetry(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  let data = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {}
  return { res, data, text };
}

async function xboxAndMinecraft(msAccessToken) {
  // 1) Xbox Live
  const xbl = await postJson('https://user.auth.xboxlive.com/user/authenticate', {
    Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: `d=${msAccessToken}` },
    RelyingParty: 'http://auth.xboxlive.com',
    TokenType: 'JWT',
  });
  if (!xbl.res.ok || !xbl.data.Token) throw new AuthError('XBL', `Xbox přihlášení selhalo (HTTP ${xbl.res.status}).`);
  const uhs = xbl.data.DisplayClaims?.xui?.[0]?.uhs;

  // 2) XSTS pro Minecraft
  const xsts = await postJson('https://xsts.auth.xboxlive.com/xsts/authorize', {
    Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.data.Token] },
    RelyingParty: 'rp://api.minecraftservices.com/',
    TokenType: 'JWT',
  });
  if (!xsts.res.ok || !xsts.data.Token) {
    const msg = XERR[xsts.data.XErr] || `Xbox odmítl přihlášení (${xsts.data.XErr || xsts.res.status}).`;
    throw new AuthError('XSTS', msg);
  }

  // 3) Minecraft
  const mc = await postJson('https://api.minecraftservices.com/authentication/login_with_xbox', {
    identityToken: `XBL3.0 x=${uhs};${xsts.data.Token}`,
  });
  if (!mc.res.ok || !mc.data.access_token) {
    if (mc.res.status === 403 || /app registration/i.test(mc.text)) {
      throw new AuthError('APP_NOT_APPROVED', 'Microsoft ještě neschválil Rainy Launcher pro Minecraft. Zatím hraj přes Minecraft Launcher.');
    }
    throw new AuthError('MC', `Minecraft přihlášení selhalo (HTTP ${mc.res.status}).`);
  }

  // 4) Profil (jméno + UUID). 404 = účet nemá koupený Minecraft
  const pr = await net.fetchWithRetry('https://api.minecraftservices.com/minecraft/profile', {
    headers: { Authorization: `Bearer ${mc.data.access_token}` },
  });
  if (pr.status === 404) throw new AuthError('NO_GAME', 'Tenhle účet nemá koupený Minecraft Java.');
  if (!pr.ok) throw new AuthError('PROFILE', `Nepodařilo se načíst profil (HTTP ${pr.status}).`);
  const profile = await pr.json();

  return {
    uuid: profile.id,
    name: profile.name,
    accessToken: mc.data.access_token,
    accessExpires: Date.now() + (mc.data.expires_in || 86400) * 1000,
    xuid: jwtClaim(mc.data.access_token, 'xuid') || '0',
  };
}

function jwtClaim(token, key) {
  try {
    const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString('utf8'));
    return payload[key] ? String(payload[key]) : null;
  } catch {
    return null;
  }
}

/** Po přihlášení v okně: code -> celý účet */
async function loginWithCode(clientId, code, pkce) {
  const ms = await msToken(clientId, { grant_type: 'authorization_code', code, code_verifier: pkce.verifier });
  const mc = await xboxAndMinecraft(ms.access_token);
  return { ...mc, refreshToken: ms.refresh_token };
}

/** Před spuštěním: obnoví token, když je starý */
async function ensureFresh(clientId, account) {
  if (account.accessToken && account.accessExpires - Date.now() > 10 * 60 * 1000) return account;
  const ms = await msToken(clientId, { grant_type: 'refresh_token', refresh_token: account.refreshToken });
  const mc = await xboxAndMinecraft(ms.access_token);
  return { ...account, ...mc, refreshToken: ms.refresh_token || account.refreshToken };
}

module.exports = { createPkce, authorizeUrl, loginWithCode, ensureFresh, AuthError, REDIRECT };
