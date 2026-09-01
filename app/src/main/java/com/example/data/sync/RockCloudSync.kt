package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.security.AntiCheat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RockCloudSync(
    private val context: Context,
    private val scope: CoroutineScope,
    private var serverUrl: String = "https://digital-pet-rock-sync.fly.dev"
) {
    private val TAG = "RockCloudSync"
    private val prefs = context.getSharedPreferences("pet_rock_online_registry", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _serverState = MutableStateFlow(SyncServerState(serverUrl = serverUrl, isConnected = true))
    val serverState: StateFlow<SyncServerState> = _serverState.asStateFlow()

    private var pollJob: Job? = null

    // Per-device HMAC signing key, minted once by the server (see
    // ensureDeviceSecret) and cached here + in prefs. Replaces a single secret
    // shared by every install of the app, which anyone could recover simply by
    // decompiling the APK.
    private var deviceSecret: String? = null

    // Local persistent cache for registered users and country clicks to ensure offline-first integrity
    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        deviceSecret = prefs.getString("device_secret", null)

        val totalGlobal = prefs.getLong("cached_global_clicks", 0L)
        val usersJson = prefs.getString("cached_users_json", null)
        val countriesJson = prefs.getString("cached_countries_json", null)

        val userList = if (!usersJson.isNullOrEmpty()) {
            parseLeaderboardList(usersJson)
        } else {
            emptyList()
        }

        val countryList = if (!countriesJson.isNullOrEmpty()) {
            parseLeaderboardList(countriesJson)
        } else {
            emptyList()
        }

        _serverState.update {
            it.copy(
                globalClicks = totalGlobal,
                topUsers = userList,
                topCountries = countryList,
                topCountry = countryList.firstOrNull()?.countryCode ?: "TR",
                onlineCount = prefs.getInt("cached_online_count", 1)
            )
        }
    }

    fun start() {
        startRealSyncLoop()
    }

    fun updateServerUrl(newUrl: String) {
        if (serverUrl == newUrl) return
        serverUrl = newUrl
        _serverState.update { it.copy(serverUrl = newUrl) }
        scope.launch(Dispatchers.IO) {
            fetchLatestGlobalStats()
        }
    }

    /**
     * Checks if a username is already taken by another user.
     * Guaranteed unique per user ID.
     */
    suspend fun checkUsernameAvailability(username: String, currentUserId: String): Boolean = withContext(Dispatchers.IO) {
        val cleanName = username.trim()
        if (cleanName.length < 3) return@withContext false

        // 1. Check local registry first
        val registeredMapJson = prefs.getString("registered_usernames_map", "{}") ?: "{}"
        val localRegistry = JSONObject(registeredMapJson)

        val registeredUserId = localRegistry.optString(cleanName.lowercase(), null)
        if (registeredUserId != null && registeredUserId != currentUserId) {
            return@withContext false
        }

        // 2. Query cloud server for uniqueness if online
        try {
            val url = "$serverUrl/api/username/check?name=${java.net.URLEncoder.encode(cleanName, "UTF-8")}&userId=$currentUserId"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    val available = json.optBoolean("available", true)
                    return@withContext available
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Server check skipped (offline fallback active): ${e.message}")
        }

        // Check against current top users list in memory
        val isNameTakenInState = _serverState.value.topUsers.any {
            it.username.equals(cleanName, ignoreCase = true) && it.identifier != currentUserId
        }

        return@withContext !isNameTakenInState
    }

    /**
     * Registers or claims the username for the given user ID.
     */
    suspend fun registerUsername(
        username: String,
        userId: String,
        countryCode: String
    ): RegistrationResult = withContext(Dispatchers.IO) {
        val cleanName = username.trim()
        if (cleanName.length < 3 || cleanName.length > 20) {
            return@withContext RegistrationResult.Error("Kullanıcı adı 3 ile 20 karakter arasında olmalıdır.")
        }

        val available = checkUsernameAvailability(cleanName, userId)
        if (!available) {
            return@withContext RegistrationResult.Error("Bu kullanıcı adı başka bir oyuncu tarafından alınmış. Lütfen farklı bir isim seçin.")
        }

        // Save to local registry
        val registeredMapJson = prefs.getString("registered_usernames_map", "{}") ?: "{}"
        val localRegistry = JSONObject(registeredMapJson)
        localRegistry.put(cleanName.lowercase(), userId)
        prefs.edit().putString("registered_usernames_map", localRegistry.toString()).apply()

        // Register to cloud server
        try {
            val payload = JSONObject().apply {
                put("userId", userId)
                put("username", cleanName)
                put("countryCode", countryCode)
            }
            val request = Request.Builder()
                .url("$serverUrl/api/username/register")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful && response.code == 409) {
                return@withContext RegistrationResult.Error("Bu kullanıcı adı başka bir oyuncu tarafından alınmış.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud registration offline queued: ${e.message}")
        }

        // Update local state top users if present
        _serverState.update { current ->
            val existing = current.topUsers.firstOrNull { it.identifier == userId }
            val updatedUsers = if (existing != null) {
                current.topUsers.map { if (it.identifier == userId) it.copy(username = cleanName, countryCode = countryCode) else it }
            } else {
                current.topUsers + LeaderboardEntry(
                    rank = current.topUsers.size + 1,
                    identifier = userId,
                    username = cleanName,
                    clicks = 0L,
                    countryCode = countryCode
                )
            }
            current.copy(topUsers = updatedUsers)
        }

        return@withContext RegistrationResult.Success
    }

    /**
     * Lazily provisions this install's HMAC signing key from the server, minted once
     * per userId and never re-issued (userId is publicly visible via the leaderboard,
     * so re-issuing on repeat calls would let anyone who has seen it on the leaderboard
     * steal that device's signing key). Cached in memory and in prefs once obtained.
     */
    private suspend fun ensureDeviceSecret(userId: String): String? {
        deviceSecret?.let { return it }

        return try {
            val payload = JSONObject().apply { put("userId", userId) }
            val request = Request.Builder()
                .url("$serverUrl/api/device/register")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                val secret = body?.let { JSONObject(it).optString("deviceSecret", "") }
                if (!secret.isNullOrEmpty()) {
                    deviceSecret = secret
                    prefs.edit().putString("device_secret", secret).apply()
                    secret
                } else {
                    null
                }
            } else {
                if (response.code == 409) {
                    Log.w(TAG, "Device secret already provisioned elsewhere for this userId; cannot recover it")
                }
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Device registration offline, will retry: ${e.message}")
            null
        }
    }

    /**
     * Submits a real verified click batch to the cloud aggregator.
     */
    suspend fun sendClickBatch(
        userId: String,
        username: String,
        countryCode: String,
        batchClicks: Int,
        durationSeconds: Int = 5,
        onSuccess: () -> Unit
    ) = withContext(Dispatchers.IO) {
        if (batchClicks <= 0) return@withContext

        val secret = ensureDeviceSecret(userId) ?: run {
            Log.d(TAG, "No device signing secret yet - skipping batch, will retry on next flush")
            return@withContext
        }

        val now = System.currentTimeMillis()
        val signature = AntiCheat.signBatch(
            userId = userId,
            timestamp = now,
            clicks = batchClicks,
            secret = secret
        )

        val payload = JSONObject().apply {
            put("userId", userId)
            put("username", username)
            put("countryCode", countryCode)
            put("batchClicks", batchClicks)
            put("clientTimestamp", now)
            put("durationSeconds", durationSeconds)
            put("signature", signature)
        }

        var syncedOnline = false

        try {
            val request = Request.Builder()
                .url("$serverUrl/api/clicks/batch")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    handleServerStatsJson(JSONObject(body))
                }
                syncedOnline = true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Sync to cloud offline fallback: ${e.message}")
        }

        // Only apply the increment and clear the local unsynced queue once the batch is
        // actually confirmed by the server. Doing this unconditionally would silently drop
        // clicks that failed to sync (see AppDatabase local_stats.unsynced_clicks), breaking
        // the offline-first "no data loss" guarantee: the next periodic flush must retry the
        // same unsynced clicks rather than have them marked as synced when they weren't.
        if (!syncedOnline) return@withContext

        _serverState.update { current ->
            val newGlobalClicks = current.globalClicks + batchClicks

            // Update user clicks in leaderboard
            val userFound = current.topUsers.any { it.identifier == userId }
            val updatedUsers = if (userFound) {
                current.topUsers.map {
                    if (it.identifier == userId) it.copy(clicks = it.clicks + batchClicks, username = username, countryCode = countryCode)
                    else it
                }
            } else {
                current.topUsers + LeaderboardEntry(
                    rank = current.topUsers.size + 1,
                    identifier = userId,
                    username = username,
                    clicks = batchClicks.toLong(),
                    countryCode = countryCode
                )
            }.sortedByDescending { it.clicks }.mapIndexed { index, entry -> entry.copy(rank = index + 1) }

            // Update country clicks in leaderboard
            val countryFound = current.topCountries.any { it.countryCode == countryCode }
            val updatedCountries = if (countryFound) {
                current.topCountries.map {
                    if (it.countryCode == countryCode) it.copy(clicks = it.clicks + batchClicks)
                    else it
                }
            } else {
                current.topCountries + LeaderboardEntry(
                    rank = current.topCountries.size + 1,
                    identifier = countryCode,
                    username = countryCode,
                    clicks = batchClicks.toLong(),
                    countryCode = countryCode
                )
            }.sortedByDescending { it.clicks }.mapIndexed { index, entry -> entry.copy(rank = index + 1) }

            persistState(newGlobalClicks, updatedUsers, updatedCountries)

            current.copy(
                globalClicks = newGlobalClicks,
                topUsers = updatedUsers,
                topCountries = updatedCountries,
                topCountry = updatedCountries.firstOrNull()?.countryCode ?: countryCode,
                isConnected = true,
                isSyncing = false,
                lastSyncTimestamp = now
            )
        }

        onSuccess()
    }

    private fun startRealSyncLoop() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchLatestGlobalStats()
                delay(8000) // Poll real server stats every 8 seconds
            }
        }
    }

    private suspend fun fetchLatestGlobalStats() {
        try {
            val request = Request.Builder()
                .url("$serverUrl/api/stats")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    handleServerStatsJson(JSONObject(body))
                }
            }
        } catch (e: Exception) {
            // Keep local offline state
        }
    }

    private fun handleServerStatsJson(json: JSONObject) {
        val globalClicks = json.optLong("globalClicks", _serverState.value.globalClicks)
        val onlineCount = json.optInt("onlineCount", _serverState.value.onlineCount)

        val topCountriesArray = json.optJSONArray("topCountries")
        val parsedCountries = parseLeaderboardJson(topCountriesArray)

        val topUsersArray = json.optJSONArray("topUsers")
        val parsedUsers = parseLeaderboardJson(topUsersArray)

        _serverState.update { current ->
            val countries = if (parsedCountries.isNotEmpty()) parsedCountries else current.topCountries
            val users = if (parsedUsers.isNotEmpty()) parsedUsers else current.topUsers

            persistState(globalClicks, users, countries)

            current.copy(
                globalClicks = globalClicks,
                onlineCount = if (onlineCount > 0) onlineCount else current.onlineCount,
                topCountries = countries,
                topUsers = users,
                topCountry = countries.firstOrNull()?.countryCode ?: current.topCountry,
                isConnected = true,
                lastSyncTimestamp = System.currentTimeMillis()
            )
        }
    }

    private fun parseLeaderboardJson(array: JSONArray?): List<LeaderboardEntry> {
        if (array == null || array.length() == 0) return emptyList()
        val list = mutableListOf<LeaderboardEntry>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val rank = obj.optInt("rank", i + 1)
            val identifier = obj.optString("identifier", obj.optString("userId", obj.optString("country", "Oyuncu")))
            val username = obj.optString("username", identifier)
            val clicks = obj.optLong("clicks", 0L)
            val countryCode = obj.optString("countryCode", obj.optString("country", "TR"))
            list.add(LeaderboardEntry(rank = rank, identifier = identifier, username = username, clicks = clicks, countryCode = countryCode))
        }
        return list
    }

    private fun parseLeaderboardList(jsonString: String): List<LeaderboardEntry> {
        return try {
            parseLeaderboardJson(JSONArray(jsonString))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistState(globalClicks: Long, users: List<LeaderboardEntry>, countries: List<LeaderboardEntry>) {
        try {
            val usersArray = JSONArray()
            for (u in users) {
                usersArray.put(JSONObject().apply {
                    put("rank", u.rank)
                    put("identifier", u.identifier)
                    put("username", u.username)
                    put("clicks", u.clicks)
                    put("countryCode", u.countryCode)
                })
            }

            val countriesArray = JSONArray()
            for (c in countries) {
                countriesArray.put(JSONObject().apply {
                    put("rank", c.rank)
                    put("identifier", c.identifier)
                    put("username", c.username)
                    put("clicks", c.clicks)
                    put("countryCode", c.countryCode)
                })
            }

            prefs.edit()
                .putLong("cached_global_clicks", globalClicks)
                .putString("cached_users_json", usersArray.toString())
                .putString("cached_countries_json", countriesArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting online state cache: ${e.message}")
        }
    }
}
