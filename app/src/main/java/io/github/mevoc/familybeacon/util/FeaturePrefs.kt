package io.github.mevoc.familybeacon.util

import android.content.Context

class FeaturePrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var smsLocationEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS, false)
        set(v) = prefs.edit().putBoolean(KEY_SMS, v).apply()

    var batteryAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY, false)
        set(v) = prefs.edit().putBoolean(KEY_BATTERY, v).apply()

    var panicEnabled: Boolean
        get() = prefs.getBoolean(KEY_PANIC, false)
        set(v) = prefs.edit().putBoolean(KEY_PANIC, v).apply()

    var geofenceEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEOFENCE, false)
        set(v) = prefs.edit().putBoolean(KEY_GEOFENCE, v).apply()

    /** Percentage threshold below which an alert is sent (default 20). */
    var batteryAlertThreshold: Int
        get() = prefs.getInt(KEY_BATTERY_THRESHOLD, 20)
        set(v) = prefs.edit().putInt(KEY_BATTERY_THRESHOLD, v).apply()

    /** Last battery % at which an alert was sent; 100 = no alert sent yet. */
    var batteryLastAlertedPct: Int
        get() = prefs.getInt(KEY_BATTERY_LAST, 100)
        set(v) = prefs.edit().putInt(KEY_BATTERY_LAST, v).apply()

    companion object {
        private const val FILE = "family_beacon_features"
        private const val KEY_SMS = "sms_location_enabled"
        private const val KEY_BATTERY = "battery_alert_enabled"
        private const val KEY_BATTERY_THRESHOLD = "battery_alert_threshold"
        private const val KEY_BATTERY_LAST = "battery_last_alerted_pct"
        private const val KEY_PANIC = "panic_enabled"
        private const val KEY_GEOFENCE = "geofence_enabled"
    }
}