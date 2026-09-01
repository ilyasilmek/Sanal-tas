'use strict';

const express = require('express');
const crypto = require('crypto');

const PORT = process.env.PORT || 8080;
const MAX_CPS = 25;
// The client reports its own durationSeconds; capping it here stops a batch
// from claiming an inflated duration just to raise its own allowed click count.
const MAX_DURATION_SECONDS = 10;
const ONLINE_WINDOW_MS = 60_000;
const LEADERBOARD_SIZE = 50;

const state = {
  globalClicks: 0,
  users: new Map(), // userId -> { userId, username, countryCode, clicks, lastSeen }
  usernamesByLower: new Map(), // lowercase username -> userId
  countries: new Map(), // countryCode -> clicks
};

// Kept separate from `state` so it can never end up serialized into a
// leaderboard/stats response by accident.
//
// Each device gets its own random signing key the first time it calls
// /api/device/register, instead of every install sharing one secret baked
// into the APK (which anyone can recover by decompiling it). userId itself
// is public - it's returned as `identifier` in leaderboard responses - so a
// device's secret is only ever handed out once; see /api/device/register.
const deviceSecrets = new Map(); // userId -> secret

function verifySignature(secret, userId, timestamp, clicks, signature) {
  const payload = `${userId}:${timestamp}:${clicks}`;
  const expected = crypto.createHmac('sha256', secret).update(payload).digest('hex');
  const expectedBuf = Buffer.from(expected, 'utf8');
  const givenBuf = Buffer.from(String(signature || ''), 'utf8');
  return expectedBuf.length === givenBuf.length && crypto.timingSafeEqual(expectedBuf, givenBuf);
}

function onlineCount() {
  const cutoff = Date.now() - ONLINE_WINDOW_MS;
  let count = 0;
  for (const user of state.users.values()) {
    if (user.lastSeen >= cutoff) count += 1;
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

app.get('/api/stats', (_req, res) => {
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

  res.json({ ok: true });
});

app.post('/api/device/register', (req, res) => {
  const userId = String((req.body || {}).userId || '');
  if (!userId) {
    return res.status(400).json({ error: 'invalid_request' });
  }
  if (deviceSecrets.has(userId)) {
    // First registration wins. A client that never persisted the secret it was
    // given (e.g. crashed right after receiving it) cannot recover it - that's
    // the trade-off for not letting a public userId double as a way to fetch
    // someone else's signing key.
    return res.status(409).json({ error: 'already_registered' });
  }

  const deviceSecret = crypto.randomBytes(32).toString('hex');
  deviceSecrets.set(userId, deviceSecret);
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

  res.json(statsPayload());
});

app.listen(PORT, () => {
  console.log(`Sanal Tas reference server listening on :${PORT}`);
});
