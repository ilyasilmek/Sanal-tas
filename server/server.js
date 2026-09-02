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
  // userId -> { userId, username, countryCode, clicks, lastSeen, daily, weekly, monthly }
  // daily/weekly/monthly are { key, clicks } buckets - see recordClicks/periodClicks.
  users: new Map(),
  usernamesByLower: new Map(), // lowercase username -> userId
  countries: new Map(), // countryCode -> { clicks, daily, weekly, monthly } (same bucket shape as users)
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

// Calendar-aligned (UTC) bucket keys for the three rolling leaderboard windows.
// Weeks are Monday-anchored; day/month are plain UTC calendar boundaries.
function periodKeys(ts) {
  const d = new Date(ts);
  const dayKey = d.toISOString().slice(0, 10); // YYYY-MM-DD
  const monthKey = dayKey.slice(0, 7); // YYYY-MM

  const dayOfWeek = (d.getUTCDay() + 6) % 7; // 0 = Monday .. 6 = Sunday
  const monday = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() - dayOfWeek));
  const weekKey = monday.toISOString().slice(0, 10);

  return { daily: dayKey, weekly: weekKey, monthly: monthKey };
}

// Adds `amount` to a record's all-time total and to each of its daily/weekly/monthly
// buckets, rolling a bucket over to 0 first if its stored key has fallen behind the
// current period (e.g. the first click of a new day/week/month).
function recordClicks(record, amount, now) {
  record.clicks = (record.clicks || 0) + amount;
  const keys = periodKeys(now);
  for (const period of ['daily', 'weekly', 'monthly']) {
    const bucket = record[period];
    const currentKey = keys[period];
    if (!bucket || bucket.key !== currentKey) {
      record[period] = { key: currentKey, clicks: amount };
    } else {
      bucket.clicks += amount;
    }
  }
}

// Reads a record's click count for a period. A bucket whose key doesn't match the
// current period is stale (nothing has been recorded in it since the period rolled
// over) and must read as 0 rather than whatever value it was last left at.
function periodClicks(record, period, now) {
  if (period === 'allTime') return record.clicks || 0;
  const bucket = record[period];
  if (!bucket) return 0;
  return bucket.key === periodKeys(now)[period] ? (bucket.clicks || 0) : 0;
}

function buildLeaderboard(map, period, isCountry, now) {
  let entries = [...map.entries()].map(([key, record]) => ({
    key,
    record,
    clicks: periodClicks(record, period, now),
  }));
  // All-time keeps zero-click registered users visible (existing behavior); a
  // per-period board only makes sense for people who actually played in it.
  if (period !== 'allTime') {
    entries = entries.filter((e) => e.clicks > 0);
  }
  entries.sort((a, b) => b.clicks - a.clicks);
  entries = entries.slice(0, LEADERBOARD_SIZE);

  return entries.map((e, index) =>
    isCountry
      ? { rank: index + 1, identifier: e.key, username: e.key, clicks: e.clicks, countryCode: e.key }
      : { rank: index + 1, identifier: e.record.userId, username: e.record.username, clicks: e.clicks, countryCode: e.record.countryCode }
  );
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
  const now = Date.now();
  const leaderboards = {};
  for (const period of ['daily', 'weekly', 'monthly', 'allTime']) {
    leaderboards[period] = {
      topUsers: buildLeaderboard(state.users, period, false, now),
      topCountries: buildLeaderboard(state.countries, period, true, now),
    };
  }

  return {
    globalClicks: state.globalClicks,
    onlineCount: onlineCount(),
    // Kept for clients that only read the flat fields; always mirrors allTime.
    topCountries: leaderboards.allTime.topCountries,
    topUsers: leaderboards.allTime.topUsers,
    leaderboards,
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
    daily: existing?.daily,
    weekly: existing?.weekly,
    monthly: existing?.monthly,
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
  const now = Date.now();

  const userRecord = state.users.get(userId) || {
    userId,
    username: String(username || `Oyuncu_${userId.slice(-4)}`),
    countryCode: country,
    clicks: 0,
  };
  userRecord.username = String(username || userRecord.username);
  userRecord.countryCode = country;
  userRecord.lastSeen = now;
  recordClicks(userRecord, batchClicks, now);
  state.users.set(userId, userRecord);

  const countryRecord = state.countries.get(country) || { clicks: 0 };
  recordClicks(countryRecord, batchClicks, now);
  state.countries.set(country, countryRecord);

  state.globalClicks += batchClicks;
  touchPresence(userId);

  saveStateToDisk();
  res.json(statsPayload());
});

app.listen(PORT, () => {
  console.log(`Sanal Tas reference server listening on :${PORT}`);
});
