package io.github.mevoc.familybeacon.util

import android.content.Context

object Prefs {
    private const val FILE = "family_beacon_prefs"
    private const val KEY_CONSENT = "consent_given"

    fun isConsentGiven(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_CONSENT, false)

    fun setConsentGiven(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CONSENT, value).apply()
    }
}