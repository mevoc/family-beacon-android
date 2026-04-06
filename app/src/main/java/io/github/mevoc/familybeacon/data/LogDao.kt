package io.github.mevoc.familybeacon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntry)

    @Insert
    fun insertSync(entry: LogEntry)

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun latest(limit: Int = 100): List<LogEntry>

    @Query("DELETE FROM events")
    suspend fun clearAll()
}