package io.github.mevoc.familybeacon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: String,   // INFO/WARN/ERROR
    val type: String,    // TOGGLE/SMS/GEOFENCE/BATTERY/PANIC
    val message: String
)