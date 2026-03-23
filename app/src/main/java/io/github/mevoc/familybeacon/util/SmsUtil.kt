package io.github.mevoc.familybeacon.util

import android.telephony.SmsManager

object SmsUtil {
    fun send(to: String, message: String) {
        SmsManager.getDefault().sendTextMessage(to, null, message, null, null)
    }
}