package io.github.mevoc.familybeacon.util

import android.content.Context
import android.telephony.SmsManager

object SmsUtil {
    fun send(context: Context, to: String, message: String) {
        @Suppress("DEPRECATION")
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(message)
        if (parts.size == 1) {
            smsManager.sendTextMessage(to, null, message, null, null)
        } else {
            smsManager.sendMultipartTextMessage(to, null, parts, null, null)
        }
    }
}