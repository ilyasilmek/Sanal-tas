'use strict';

const express = require('express');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8080;
const MAX_CPS = 25;
const MAX_DURATION_SECONDS = 10;
const ONLINE_WINDOW_MS = 60_000;
const LEADERBOARD_SIZE = 50;
const STATE_FILE = path.join(__dirname, 'server_state.json');

const state = {
  globalClicks: 0,
  users: new Map(), // userId -> { userId, username, countryCode, clicks, lastSeen }
  usernamesByLower: new Map(), // lowercase username -> userId
  countries: new Map(), // countryCode -> clicks
  presence: new Map(), // userId -> lastSeen timestamp, refreshed by any open app (not just click batches)
};

const deviceSecrets = new Map(); // userId -> secret

function saveStateToDisk() {
  try {
    const data = {
      globalClicks: state.globalClicks,
      users: [...state.users.entries()],
      usernamesByLower: [...state.usernamesByLower.entries()],
      countries: [...state.countries.entries()],
      deviceSecrets: [...deviceSecrets.entries()],
    };
    fs.writeFileSync(STATE_FILE, JSON.stringify(data, null, 2));
  } catch (err) {
    console.error('Failed to save state to disk:', err.message);
  }
}

function loadStateFromDisk() {
  try {
    if (fs.existsSync(STATE_FILE)) {
      const raw = fs.readFileSync(STATE_FILE, 'utf8');
      const data = JSON.parse(raw);
      if (data.globalClicks != null) state.globalClicks = data.globalClicks;
      if (Array.isArray(data.users)) state.users = new Map(data.users);
      if (Array.isArray(data.usernamesByLower)) state.usernamesByLower = new Map(data.usernamesByLower);
      if (Array.isArray(data.countries)) state.countries = new Map(data.countries);
      if (Array.isArray(data.deviceSecrets)) {
        for (const [k, v] of data.deviceSecrets) deviceSecrets.set(k, v);
      }
      console.log(`Loaded state: ${state.globalClicks} global clicks, ${state.users.size} users from disk.`);
    }
  } catch (err) {
    console.error('Failed to load state from disk:', err.message);
  }
}

loadStateFromDisk();

function verifySignature(secret, userId, timestamp, clicks, signature) {
  const payload = `${userId}:${timestamp}:${clicks}`;
  const expected = crypto.createHmac('sha256', secret).update(payload).digest('hex');
  const expectedBuf = Buffer.from(expected, 'utf8');
  const givenBuf = Buffer.from(String(signature || ''), 'utf8');
  return expectedBuf.length === givenBuf.length && crypto.timingSafeEqual(expectedBuf, givenBuf);
}

function touchPresence(userId) {
  if (!userId) return;
  state.presence.set(userId, Date.now());
}

function onlineCount() {
  const cutoff = Date.now() - ONLINE_WINDOW_MS;
  let count = 0;
  for (const [userId, lastSeen] of state.presence) {
    if (lastSeen >= cutoff) {
      count += 1;
    } else {
      state.presence.delete(userId);
    }
  }
  return Math.max(count, 1);
}

function statsPayload() {
  const topUsers = [...state.users.values()]
    .sort((a, b) => b.clicks - a.clicks)
    .slice(0, LEADERBOARD_SIZE)
    .map((user, index) => ({
      rank: index + 1,
      identifier: user.userId,
      username: user.username,
      clicks: user.clicks,
      countryCode: user.countryCode,
    }));

  const topCountries = [...state.countries.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, LEADERBOARD_SIZE)
    .map(([countryCode, clicks], index) => ({
      rank: index + 1,
      identifier: countryCode,
      username: countryCode,
      clicks,
      countryCode,
    }));

  return {
    globalClicks: state.globalClicks,
    onlineCount: onlineCount(),
    topCountries,
    topUsers,
  };
}

const app = express();
app.use(express.json());

app.get('/health', (_req, res) => res.json({ ok: true }));

app.get('/api/stats', (req, res) => {
  // Every open app polls this endpoint on a fixed interval regardless of tap
  // activity, so it's the only reliable heartbeat for presence - relying on
  // click batches alone undercounts idle-but-open devices.
  touchPresence(String(req.query.userId || ''));
  res.json(statsPayload());
});

app.get('/api/username/check', (req, res) => {
  const name = String(req.query.name || '').trim();
  const userId = String(req.query.userId || '');
  if (name.length < 3) {
    return res.json({ available: false });
  }

  const owner = state.usernamesByLower.get(name.toLowerCase());
  res.json({ available: !owner || owner === userId });
});

app.post('/api/username/register', (req, res) => {
  const { userId, username, countryCode } = req.body || {};
  const cleanName = String(username || '').trim();
  if (!userId || cleanName.length < 3 || cleanName.length > 20) {
    return res.status(400).json({ error: 'invalid_request' });
  }

  const key = cleanName.toLowerCase();
  const owner = state.usernamesByLower.get(key);
  if (owner && owner !== userId) {
    return res.status(409).json({ error: 'username_taken' });
  }

  state.usernamesByLower.set(key, userId);

  const existing = state.users.get(userId);
  state.users.set(userId, {
    userId,
    username: cleanName,
    countryCode: String(countryCode || existing?.countryCode || 'TR').toUpperCase(),
    clicks: existing?.clicks || 0,
    lastSeen: Date.now(),
  });
  touchPresence(userId);

  saveStateToDisk();
  res.json({ ok: true });
});

app.post('/api/device/register', (req, res) => {
  const userId = String((req.body || {}).userId || '');
  if (!userId) {
    return res.status(400).json({ error: 'invalid_request' });
  }
  if (deviceSecrets.has(userId)) {
    return res.status(200).json({ deviceSecret: deviceSecrets.get(userId) });
  }

  const deviceSecret = crypto.randomBytes(32).toString('hex');
  deviceSecrets.set(userId, deviceSecret);
  saveStateToDisk();
  res.status(201).json({ deviceSecret });
});

app.post('/api/clicks/batch', (req, res) => {
  const { userId, username, countryCode, batchClicks, clientTimestamp, durationSeconds, signature } = req.body || {};

  if (!userId || !Number.isInteger(batchClicks) || batchClicks <= 0) {
    return res.status(400).json({ error: 'invalid_batch' });
  }

  const deviceSecret = deviceSecrets.get(userId);
  if (!deviceSecret) {
    return res.status(401).json({ error: 'device_not_registered' });
  }
  if (!verifySignature(deviceSecret, userId, clientTimestamp, batchClicks, signature)) {
    return res.status(401).json({ error: 'invalid_signature' });
  }

  const effectiveDuration = Math.min(Math.max(Number(durationSeconds) || 1, 1), MAX_DURATION_SECONDS);
  const maxAllowed = effectiveDuration * MAX_CPS;
  if (batchClicks > maxAllowed) {
    return res.status(400).json({ error: 'rate_limit_exceeded', maxAllowed });
  }

  const country = String(countryCode || 'TR').toUpperCase();
  const existing = state.users.get(userId);
  state.users.set(userId, {
    userId,
    username: String(username || existing?.username || `Oyuncu_${userId.slice(-4)}`),
    countryCode: country,
    clicks: (existing?.clicks || 0) + batchClicks,
    lastSeen: Date.now(),
  });
  state.countries.set(country, (state.countries.get(country) || 0) + batchClicks);
  state.globalClicks += batchClicks;
  touchPresence(userId);

  saveStateToDisk();
  res.json(statsPayload());
});

app.listen(PORT, () => {
  console.log(`Sanal Tas reference server listening on :${PORT}`);
});
