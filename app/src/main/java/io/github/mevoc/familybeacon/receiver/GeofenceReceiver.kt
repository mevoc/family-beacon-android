package io.github.mevoc.familybeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.util.ContactStore
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.SafeZoneStore
import io.github.mevoc.familybeacon.util.SmsUtil

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = FeaturePrefs(context)
        if (!prefs.geofenceEnabled) return

        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            EventLogger.error(context, "GEOFENCE", "GeofencingEvent error code: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val label = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "entered"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "exited"
            else -> {
                EventLogger.warn(context, "GEOFENCE", "Unknown transition type: $transition")
                return
            }
        }

        val zoneNames = event.triggeringGeofences
            ?.mapNotNull { SafeZoneStore(context).findById(it.requestId)?.name?.takeIf { n -> n.isNotEmpty() } ?: it.requestId }
            ?.joinToString(", ") ?: "unknown"

        val msg = "🗺️ Geofence: $label \"$zoneNames\"."
        val recipients = ContactStore(context).allReceivingGeofence()

        if (recipients.isEmpty()) {
            EventLogger.warn(context, "GEOFENCE", "No contacts with geofence alerts enabled — alert not sent ($label)")
            return
        }

        recipients.forEach { SmsUtil.send(it.number, msg) }
        EventLogger.warn(context, "GEOFENCE", "Alert sent ($label) to ${recipients.size} contact(s)")
    }
}
