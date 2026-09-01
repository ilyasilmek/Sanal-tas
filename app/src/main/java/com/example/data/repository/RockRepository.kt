package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.LocalStats
import com.example.data.sync.RegistrationResult
import com.example.data.sync.RockCloudSync
import com.example.data.sync.SyncServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class RockRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("pet_rock_prefs", Context.MODE_PRIVATE)

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "pet_rock.db"
    ).build()

    private val statsDao = db.statsDao()
    val localStatsFlow: Flow<LocalStats?> = statsDao.getLocalStatsFlow()

    val cloudSync = RockCloudSync(context, scope)
    val serverState: StateFlow<SyncServerState> = cloudSync.serverState

    private var syncJob: Job? = null

    init {
        scope.launch(Dispatchers.IO) {
            initDatabase()
            cloudSync.start()
            startPeriodicBatchSync()
        }
    }

    private suspend fun initDatabase() {
        val existing = statsDao.getLocalStats()
        if (existing == null) {
            statsDao.insertOrUpdate(
                LocalStats(
                    id = 1,
                    totalClicks = 0L,
                    unsyncedClicks = 0L,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun getUserId(): String {
        var userId = prefs.getString("user_id", null)
        if (userId == null) {
            userId = "usr_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            prefs.edit().putString("user_id", userId).apply()
        }
        return userId
    }

    fun getUsername(): String? {
        return prefs.getString("user_name", null)
    }

    fun isUserRegistered(): Boolean {
        val name = getUsername()
        return !name.isNullOrBlank()
    }

    suspend fun registerUser(username: String, countryCode: String): RegistrationResult {
        val userId = getUserId()
        val result = cloudSync.registerUsername(username, userId, countryCode)
        if (result is RegistrationResult.Success) {
            prefs.edit()
                .putString("user_name", username.trim())
                .putString("country_code", countryCode.uppercase())
                .apply()
            // Flush any clicks recorded prior to registration
            flushUnsyncedClicks()
        }
        return result
    }

    fun getCountryCode(): String {
        return prefs.getString("country_code", "TR") ?: "TR"
    }

    fun setCountryCode(code: String) {
        prefs.edit().putString("country_code", code.uppercase()).apply()
    }

    suspend fun incrementClick() {
        withContext(Dispatchers.IO) {
            statsDao.incrementClicks(1L)
        }
    }

    private fun startPeriodicBatchSync() {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5000)
                flushUnsyncedClicks()
            }
        }
    }

    suspend fun flushUnsyncedClicks() {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getLocalStats() ?: return@withContext
            val unsynced = stats.unsyncedClicks.toInt()
            if (unsynced > 0) {
                val userId = getUserId()
                val username = getUsername() ?: "Oyuncu_${userId.takeLast(4)}"
                val country = getCountryCode()

                cloudSync.sendClickBatch(
                    userId = userId,
                    username = username,
                    countryCode = country,
                    batchClicks = unsynced,
                    durationSeconds = 5,
                    onSuccess = {
                        // Decrement by exactly what this batch covered, not a hard reset to 0 -
                        // taps recorded while the request was in flight must stay queued for the
                        // next flush instead of being wiped out along with the synced ones.
                        scope.launch(Dispatchers.IO) {
                            statsDao.decrementUnsyncedClicks(unsynced.toLong())
                        }
                    }
                )
            }
        }
    }

    suspend fun getLocalStatsSnapshot(): LocalStats {
        return withContext(Dispatchers.IO) {
            statsDao.getLocalStats() ?: LocalStats(1, 0, 0, System.currentTimeMillis())
        }
    }
}
