package io.github.mevoc.familybeacon.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SafeZoneStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun getAll(): List<SafeZone> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getJSONObject(it).toSafeZone() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(zones: List<SafeZone>) {
        val arr = JSONArray()
        zones.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(zone: SafeZone) = save(getAll() + zone)

    fun remove(id: String) = save(getAll().filter { it.id != id })

    fun update(zone: SafeZone) = save(getAll().map { if (it.id == zone.id) zone else it })

    fun findById(id: String): SafeZone? = getAll().find { it.id == id }

    companion object {
        private const val PREFS_FILE = "family_beacon_safe_zones"
        private const val KEY = "zones_json"
    }
}

private fun JSONObject.toSafeZone() = SafeZone(
    id = getString("id"),
    name = optString("name", ""),
    lat = getDouble("lat"),
    lng = getDouble("lng"),
    radiusMeters = getDouble("radius").toFloat(),
    alertOnEnter = optBoolean("enter", true),
    alertOnExit = optBoolean("exit", true)
)

private fun SafeZone.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("lat", lat)
    put("lng", lng)
    put("radius", radiusMeters.toDouble())
    put("enter", alertOnEnter)
    put("exit", alertOnExit)
}
