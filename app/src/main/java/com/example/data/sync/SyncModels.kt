package com.example.data.sync

data class LeaderboardEntry(
    val rank: Int,
    val identifier: String,
    val username: String = identifier,
    val clicks: Long,
    val countryCode: String = "TR"
)

enum class LeaderboardPeriod(val apiKey: String, val label: String) {
    DAILY("daily", "Günlük"),
    WEEKLY("weekly", "Haftalık"),
    MONTHLY("monthly", "Aylık"),
    ALL_TIME("allTime", "Tüm Zamanlar")
}

data class PeriodLeaderboard(
    val topUsers: List<LeaderboardEntry> = emptyList(),
    val topCountries: List<LeaderboardEntry> = emptyList()
)

data class SyncServerState(
    val globalClicks: Long = 0L,
    val topCountry: String = "TR",
    val topCountries: List<LeaderboardEntry> = emptyList(),
    val topUsers: List<LeaderboardEntry> = emptyList(),
    val leaderboardsByPeriod: Map<LeaderboardPeriod, PeriodLeaderboard> = emptyMap(),
    val onlineCount: Int = 1,
    val isConnected: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val serverUrl: String = "https://sanal-tas.onrender.com"
)

sealed class RegistrationResult {
    data object Success : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}
