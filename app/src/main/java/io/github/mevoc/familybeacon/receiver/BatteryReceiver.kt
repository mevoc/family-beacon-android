package io.github.mevoc.familybeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.util.ContactStore
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.SmsUtil

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = FeaturePrefs(context)
        if (!prefs.batteryAlertEnabled) return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return

        val percent = (level * 100 / scale)
        val threshold = prefs.batteryAlertThreshold
        val lastAlerted = prefs.batteryLastAlertedPct

        // Reset throttle once battery rises back above the threshold
        if (percent > threshold) {
            prefs.batteryLastAlertedPct = 100
            return
        }

        // Only alert once per drop below threshold
        if (lastAlerted <= threshold) return

        prefs.batteryLastAlertedPct = threshold

        val msg = "🔋 Battery alert: $percent% remaining (threshold: $threshold%)."
        val recipients = ContactStore(context).allReceivingBattery()

        if (recipients.isEmpty()) {
            EventLogger.warn(context, "BATTERY", "No contacts with battery alerts enabled — alert not sent ($percent%)")
            return
        }

        recipients.forEach { SmsUtil.send(context, it.number, msg) }
        EventLogger.warn(context, "BATTERY", "Alert sent at $percent% (threshold $threshold%) to ${recipients.size} contact(s)")
    }
}
