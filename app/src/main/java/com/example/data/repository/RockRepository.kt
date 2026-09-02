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
            cloudSync.start(getUserId())
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
            initDatabase()
            statsDao.incrementClicks(1L)
        }
    }

    private fun startPeriodicBatchSync() {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5000)
                reconcileUnsyncedClicks()
                flushUnsyncedClicks()
            }
        }
    }

    suspend fun reconcileUnsyncedClicks() {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getLocalStats() ?: return@withContext
            if (stats.totalClicks <= 0) return@withContext

            val userId = getUserId()
            val userEntry = cloudSync.serverState.value.topUsers.find { it.identifier == userId }
            val serverUserClicks = userEntry?.clicks ?: 0L

            val missingFromCloud = stats.totalClicks - (serverUserClicks + stats.unsyncedClicks)
            if (missingFromCloud > 0) {
                statsDao.addUnsyncedClicks(missingFromCloud)
            }
        }
    }

    suspend fun flushUnsyncedClicks() {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getLocalStats() ?: return@withContext
            var remainingUnsynced = stats.unsyncedClicks

            if (remainingUnsynced > 0) {
                val userId = getUserId()
                val username = getUsername() ?: "Oyuncu_${userId.takeLast(4)}"
                val country = getCountryCode()

                while (remainingUnsynced > 0) {
                    val batchSize = minOf(remainingUnsynced, 100L).toInt()
                    var batchSuccess = false

                    cloudSync.sendClickBatch(
                        userId = userId,
                        username = username,
                        countryCode = country,
                        batchClicks = batchSize,
                        durationSeconds = 5,
                        onSuccess = {
                            batchSuccess = true
                        }
                    )

                    if (batchSuccess) {
                        statsDao.decrementUnsyncedClicks(batchSize.toLong())
                        remainingUnsynced -= batchSize
                    } else {
                        // Stop chunking on failure and retry in next sync loop
                        break
                    }
                }
            }
        }
    }

    suspend fun getLocalStatsSnapshot(): LocalStats {
        return withContext(Dispatchers.IO) {
            statsDao.getLocalStats() ?: LocalStats(1, 0, 0, System.currentTimeMillis())
        }
    }
}
