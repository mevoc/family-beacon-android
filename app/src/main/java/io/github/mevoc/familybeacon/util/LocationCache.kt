package io.github.mevoc.familybeacon.util

import android.content.Context

object LocationCache {
    private const val FILE = "family_beacon_loc"
    private const val KEY_LAST = "last_latlon"
    private const val KEY_TIME = "last_time"
    private const val KEY_ACC = "last_acc"

    fun save(context: Context, lat: Double, lon: Double, accMeters: Float?, timeMs: Long) {
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST, "$lat,$lon")
            .putLong(KEY_TIME, timeMs)
            .putFloat(KEY_ACC, accMeters ?: -1f)
            .apply()
    }

    data class Cached(val lat: Double, val lon: Double, val timeMs: Long, val acc: Float?)

    fun get(context: Context): Cached? {
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val s = p.getString(KEY_LAST, null) ?: return null
        val parts = s.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        val time = p.getLong(KEY_TIME, 0L)
        val accRaw = p.getFloat(KEY_ACC, -1f)
        val acc = if (accRaw >= 0f) accRaw else null
        return Cached(lat, lon, time, acc)
    }
}