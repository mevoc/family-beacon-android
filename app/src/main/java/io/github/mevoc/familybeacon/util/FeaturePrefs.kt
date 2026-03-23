package io.github.mevoc.familybeacon.util

import android.content.Context

class FeaturePrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var smsLocationEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS, false)
        set(v) = prefs.edit().putBoolean(KEY_SMS, v).apply()

    var whitelistCsv: String
        get() = prefs.getString(KEY_WHITELIST, "") ?: ""
        set(v) = prefs.edit().putString(KEY_WHITELIST, v).apply()

    fun isWhitelisted(number: String): Boolean {
        val normalized = normalize(number)
        val allowed = whitelistCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { normalize(it) }
            .toSet()
        return normalized in allowed
    }

    private fun normalize(n: String): String =
        n.replace(" ", "").replace("-", "")

    var batteryAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY, false)
        set(v) = prefs.edit().putBoolean(KEY_BATTERY, v).apply()

    var panicEnabled: Boolean
        get() = prefs.getBoolean(KEY_PANIC, false)
        set(v) = prefs.edit().putBoolean(KEY_PANIC, v).apply()

    var geofenceEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEOFENCE, false)
        set(v) = prefs.edit().putBoolean(KEY_GEOFENCE, v).apply()

    companion object {
        private const val FILE = "family_beacon_features"
        private const val KEY_SMS = "sms_location_enabled"
        private const val KEY_WHITELIST = "+16505556789,+4673yyyyyyy"
        private const val KEY_BATTERY = "battery_alert_enabled"
        private const val KEY_PANIC = "panic_enabled"
        private const val KEY_GEOFENCE = "geofence_enabled"
    }
}