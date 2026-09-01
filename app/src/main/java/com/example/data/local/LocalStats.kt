package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_stats")
data class LocalStats(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "total_clicks") val totalClicks: Long = 0L,
    @ColumnInfo(name = "unsynced_clicks") val unsyncedClicks: Long = 0L,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface StatsDao {
    @Query("SELECT * FROM local_stats WHERE id = 1 LIMIT 1")
    fun getLocalStatsFlow(): Flow<LocalStats?>

    @Query("SELECT * FROM local_stats WHERE id = 1 LIMIT 1")
    suspend fun getLocalStats(): LocalStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: LocalStats)

    @Query("UPDATE local_stats SET total_clicks = total_clicks + :amount, unsynced_clicks = unsynced_clicks + :amount, last_updated = :timestamp WHERE id = 1")
    suspend fun incrementClicks(amount: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE local_stats SET unsynced_clicks = MAX(unsynced_clicks - :amount, 0), last_updated = :timestamp WHERE id = 1")
    suspend fun decrementUnsyncedClicks(amount: Long, timestamp: Long = System.currentTimeMillis())
}

@Database(entities = [LocalStats::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
}
