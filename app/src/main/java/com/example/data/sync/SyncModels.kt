package com.example.data.sync

data class LeaderboardEntry(
    val rank: Int,
    val identifier: String,
    val username: String = identifier,
    val clicks: Long,
    val countryCode: String = "TR"
)

data class SyncServerState(
    val globalClicks: Long = 0L,
    val topCountry: String = "TR",
    val topCountries: List<LeaderboardEntry> = emptyList(),
    val topUsers: List<LeaderboardEntry> = emptyList(),
    val onlineCount: Int = 1,
    val isConnected: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val serverUrl: String = "https://digital-pet-rock-sync.fly.dev"
)

sealed class RegistrationResult {
    data object Success : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}
