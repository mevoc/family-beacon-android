package io.github.mevoc.familybeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.service.PanicService
import io.github.mevoc.familybeacon.util.ContactStore
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.LocationUtil
import io.github.mevoc.familybeacon.util.SmsUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        EventLogger.info(context, "SMS", "SmsReceiver fired (action=${intent.action})")

        val prefs = FeaturePrefs(context)
        if (!prefs.smsLocationEnabled && !prefs.panicEnabled) {
            EventLogger.warn(context, "SMS", "Receiver: both features disabled, ignoring")
            return
        }

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isEmpty()) {
            EventLogger.warn(context, "SMS", "Receiver: no messages in intent")
            return
        }

        val from = msgs.first().originatingAddress
        if (from == null) {
            EventLogger.warn(context, "SMS", "Receiver: null originating address")
            return
        }

        val body = msgs.joinToString("") { it.messageBody ?: "" }.trim()

        // goAsync() extends the receiver's lifetime so the coroutine (and EventLogger) can complete
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                process(context, from, body, prefs)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun process(context: Context, from: String, body: String, prefs: FeaturePrefs) {
        val store = ContactStore(context)
        val contact = store.findByNumber(from)
        if (contact == null) {
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

    // Suspend until location is obtained and reply SMS sent, so pending.finish() waits for it
    private suspend fun handleLoc(context: Context, replyTo: String, name: String) {
        EventLogger.info(context, "SMS", "POS requested by $name ($replyTo)")

        suspendCancellableCoroutine<Unit> { cont ->
            // FusedLocationClient needs to be called from a thread with a Looper
            Handler(Looper.getMainLooper()).post {
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
                        if (cont.isActive) cont.resume(Unit)
                    },
                    onFailure = { reason ->
                        SmsUtil.send(context, replyTo, "⚠️ Could not get location ($reason).")
                        EventLogger.error(context, "SMS", "POS failed for $name: $reason")
                        if (cont.isActive) cont.resume(Unit)
                    }
                )
            }
        }
    }

    private suspend fun handlePanic(context: Context, from: String, name: String) {
        EventLogger.warn(context, "PANIC", "PANIC triggered by $name ($from)")
        withContext(Dispatchers.Main) {
            context.startService(Intent(context, PanicService::class.java))
        }
    }

    private suspend fun handlePanicStop(context: Context, from: String, name: String) {
        EventLogger.info(context, "PANIC", "PANIC STOP triggered by $name ($from)")
        withContext(Dispatchers.Main) {
            context.stopService(Intent(context, PanicService::class.java))
        }
    }
}
