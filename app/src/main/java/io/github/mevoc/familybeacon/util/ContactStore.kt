package io.github.mevoc.familybeacon.util

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import org.json.JSONArray
import org.json.JSONObject

class ContactStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

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
        val clean = clean(contact.number)
        val list = getAll().toMutableList()
        if (list.none { matchKey(it.number) == matchKey(clean) }) {
            list.add(contact.copy(number = clean))
            save(list)
        }
    }

    fun remove(number: String) {
        val key = matchKey(number)
        save(getAll().filter { matchKey(it.number) != key })
    }

    fun update(contact: Contact) {
        val key = matchKey(contact.number)
        save(getAll().map {
            if (matchKey(it.number) == key) contact.copy(number = clean(contact.number)) else it
        })
    }

    fun isWhitelisted(number: String): Boolean = findByNumber(number) != null

    fun findByNumber(number: String): Contact? {
        val key = matchKey(number)
        return getAll().find { matchKey(it.number) == key }
    }

    fun allReceivingBattery() = getAll().filter { it.receiveBattery }
    fun allReceivingGeofence() = getAll().filter { it.receiveGeofence }

    /**
     * Expands to E.164 format using the SIM's country if possible,
     * otherwise strips formatting and stores as-is.
     */
    fun clean(n: String): String {
        val stripped = n.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        val countryIso = (appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
            ?.networkCountryIso?.uppercase()
        if (!countryIso.isNullOrEmpty()) {
            val e164 = PhoneNumberUtils.formatNumberToE164(stripped, countryIso)
            if (!e164.isNullOrEmpty()) return e164
        }
        return stripped
    }

    /** Last 9 digits for comparison only — lets "5554" match "+15554". */
    private fun matchKey(n: String): String {
        val s = n.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        return if (s.length > 9) s.takeLast(9) else s
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
