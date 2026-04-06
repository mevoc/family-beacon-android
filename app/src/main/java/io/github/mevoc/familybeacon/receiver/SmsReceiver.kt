package io.github.mevoc.familybeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.service.PanicService
import io.github.mevoc.familybeacon.util.ContactStore
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.LocationUtil
import io.github.mevoc.familybeacon.util.SmsUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = FeaturePrefs(context)
        if (!prefs.smsLocationEnabled && !prefs.panicEnabled) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isEmpty()) return

        val from = msgs.first().originatingAddress ?: return
        val body = msgs.joinToString("") { it.messageBody ?: "" }.trim()

        val store = ContactStore(context)
        val contact = store.findByNumber(from)
        if (contact == null) {
            // Only log if it looks like a command attempt, to avoid noise from normal SMS
            val cmd = body.uppercase(Locale.ROOT)
            if (cmd == "POS" || cmd.startsWith("PANIC")) {
                EventLogger.warn(context, "SMS", "Command '$cmd' from unknown number: $from")
            }
            return
        }

        when (body.uppercase(Locale.ROOT)) {
            "POS" -> when {
                !prefs.smsLocationEnabled -> EventLogger.warn(context, "SMS", "POS ignored — feature disabled")
                !contact.canRequestPosition -> EventLogger.warn(context, "SMS", "POS denied for ${contact.name} ($from)")
                else -> handleLoc(context, from, contact.name)
            }
            "PANIC" -> when {
                !prefs.panicEnabled -> EventLogger.warn(context, "SMS", "PANIC ignored — feature disabled")
                !contact.canRequestPanic -> EventLogger.warn(context, "SMS", "PANIC denied for ${contact.name} ($from)")
                else -> handlePanic(context, from, contact.name)
            }
            "PANIC STOP" -> when {
                !prefs.panicEnabled -> EventLogger.warn(context, "SMS", "PANIC STOP ignored — feature disabled")
                !contact.canRequestPanic -> EventLogger.warn(context, "SMS", "PANIC STOP denied for ${contact.name} ($from)")
                else -> handlePanicStop(context, from, contact.name)
            }
            else -> { /* not a recognised command — ignore silently */ }
        }
    }

    private fun handleLoc(context: Context, replyTo: String, name: String) {
        EventLogger.info(context, "SMS", "POS requested by $name ($replyTo)")

        LocationUtil.requestBestEffortLocation(
            context = context,
            onResult = { lat, lon, acc, timeMs, source ->
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val time = sdf.format(Date(timeMs))
                val accTxt = acc?.toInt()?.let { "±${it}m" } ?: "unknown"
                val maps = "https://maps.google.com/?q=$lat,$lon"

                val msg = buildString {
                    append("📍 $lat,$lon\n")
                    append("acc: $accTxt\n")
                    append("time: $time\n")
                    append("src: $source\n")
                    append(maps)
                }

                SmsUtil.send(context, replyTo, msg)
                EventLogger.info(context, "SMS", "Replied POS to $name ($source, $accTxt)")
            },
            onFailure = { reason ->
                SmsUtil.send(context, replyTo, "⚠️ Could not get location ($reason).")
                EventLogger.error(context, "SMS", "POS failed for $name: $reason")
            }
        )
    }

    private fun handlePanic(context: Context, from: String, name: String) {
        EventLogger.warn(context, "PANIC", "PANIC triggered by $name ($from)")
        context.startService(Intent(context, PanicService::class.java))
    }

    private fun handlePanicStop(context: Context, from: String, name: String) {
        EventLogger.info(context, "PANIC", "PANIC STOP triggered by $name ($from)")
        context.stopService(Intent(context, PanicService::class.java))
    }
}
