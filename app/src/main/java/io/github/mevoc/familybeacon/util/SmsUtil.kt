package io.github.mevoc.familybeacon.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import io.github.mevoc.familybeacon.data.EventLogger

object SmsUtil {
    private const val ACTION_SENT = "io.github.mevoc.familybeacon.SMS_SENT"

    fun send(context: Context, to: String, message: String) {
        try {
            @Suppress("DEPRECATION")
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            if (smsManager == null) {
                EventLogger.error(context, "SMS", "SmsManager is null — cannot send to $to")
                return
            }

            // Register a one-shot receiver to log delivery result
            val sentIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_SENT),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    ctx.unregisterReceiver(this)
                    if (resultCode == android.app.Activity.RESULT_OK) {
                        EventLogger.info(ctx, "SMS", "Send OK → $to")
                    } else {
                        EventLogger.error(ctx, "SMS", "Send FAILED (code $resultCode) → $to")
                    }
                }
            }, IntentFilter(ACTION_SENT), Context.RECEIVER_NOT_EXPORTED)

            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(to, null, message, sentIntent, null)
            } else {
                val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                    add(sentIntent)
                    repeat(parts.size - 1) { add(null) }
                }
                smsManager.sendMultipartTextMessage(to, null, parts, sentIntents, null)
            }
        } catch (e: Exception) {
            EventLogger.error(context, "SMS", "send() exception: ${e.message} → $to")
        }
    }
}
