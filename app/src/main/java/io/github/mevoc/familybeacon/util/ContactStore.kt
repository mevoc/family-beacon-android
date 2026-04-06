package io.github.mevoc.familybeacon.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ContactStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun getAll(): List<Contact> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getJSONObject(it).toContact() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(contacts: List<Contact>) {
        val arr = JSONArray()
        contacts.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(contact: Contact) {
        val normalized = normalize(contact.number)
        val list = getAll().toMutableList()
        if (list.none { normalize(it.number) == normalized }) {
            list.add(contact.copy(number = normalized))
            save(list)
        }
    }

    fun remove(number: String) {
        val normalized = normalize(number)
        save(getAll().filter { normalize(it.number) != normalized })
    }

    fun update(contact: Contact) {
        val normalized = normalize(contact.number)
        save(getAll().map {
            if (normalize(it.number) == normalized) contact.copy(number = normalized) else it
        })
    }

    fun isWhitelisted(number: String): Boolean {
        val normalized = normalize(number)
        return getAll().any { normalize(it.number) == normalized }
    }

    fun findByNumber(number: String): Contact? {
        val normalized = normalize(number)
        return getAll().find { normalize(it.number) == normalized }
    }

    fun allReceivingBattery() = getAll().filter { it.receiveBattery }
    fun allReceivingGeofence() = getAll().filter { it.receiveGeofence }

    fun normalize(n: String): String {
        val stripped = n.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        // Strip leading +<country code> or leading 00<country code> so "5554" matches "+15554"
        // Keep last 9 digits for comparison (covers most country/subscriber combos)
        return if (stripped.length > 9) stripped.takeLast(9) else stripped
    }

    companion object {
        private const val PREFS_FILE = "family_beacon_contacts"
        private const val KEY = "contacts_json"
    }
}

private fun JSONObject.toContact() = Contact(
    name = optString("name", ""),
    number = getString("number"),
    canRequestPosition = optBoolean("canPos", true),
    receiveBattery = optBoolean("canBat", true),
    canRequestPanic = optBoolean("canPanic", true),
    receiveGeofence = optBoolean("canGeo", true)
)

private fun Contact.toJson() = JSONObject().apply {
    put("name", name)
    put("number", number)
    put("canPos", canRequestPosition)
    put("canBat", receiveBattery)
    put("canPanic", canRequestPanic)
    put("canGeo", receiveGeofence)
}
