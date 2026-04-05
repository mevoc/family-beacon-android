package io.github.mevoc.familybeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.service.PanicService
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

        if (!prefs.isWhitelisted(from)) {
            EventLogger.warn(context, "SMS", "Rejected SMS from non-whitelisted: $from")
            return
        }

        when (body.uppercase(Locale.ROOT)) {
            "LOC" -> if (prefs.smsLocationEnabled) handleLoc(context, from)
                     else EventLogger.warn(context, "SMS", "LOC ignored — feature disabled")
            "PANIC" -> if (prefs.panicEnabled) handlePanic(context, from)
                       else EventLogger.warn(context, "SMS", "PANIC ignored — feature disabled")
            "PANIC STOP" -> if (prefs.panicEnabled) handlePanicStop(context, from)
                            else EventLogger.warn(context, "SMS", "PANIC STOP ignored — feature disabled")
            else -> EventLogger.info(context, "SMS", "Ignored command '$body' from $from")
        }
    }

    private fun handleLoc(context: Context, replyTo: String) {
        EventLogger.info(context, "SMS", "LOC requested by $replyTo")

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

                SmsUtil.send(replyTo, msg)
                EventLogger.info(context, "SMS", "Replied LOC to $replyTo ($source, $accTxt)")
            },
            onFailure = { reason ->
                SmsUtil.send(replyTo, "⚠️ Could not get location ($reason).")
                EventLogger.error(context, "SMS", "LOC failed: $reason")
            }
        )
    }

    private fun handlePanic(context: Context, from: String) {
        EventLogger.warn(context, "PANIC", "PANIC triggered by $from")
        context.startService(Intent(context, PanicService::class.java))
    }

    private fun handlePanicStop(context: Context, from: String) {
        EventLogger.info(context, "PANIC", "PANIC STOP triggered by $from")
        context.stopService(Intent(context, PanicService::class.java))
    }
}