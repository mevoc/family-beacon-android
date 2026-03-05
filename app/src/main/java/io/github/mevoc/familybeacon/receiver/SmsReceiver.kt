package io.github.mevoc.familybeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.util.FeaturePrefs

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = FeaturePrefs(context)
        if (!prefs.smsLocationEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val from = messages.first().originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }.trim()

        // TODO: whitelist-check + commands
        EventLogger.info(context, "SMS", "Incoming SMS from $from: '$body' (stub)")
    }
}