package io.github.mevoc.familybeacon.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.receiver.GeofenceReceiver
import io.github.mevoc.familybeacon.util.SafeZoneStore

object GeofenceHelper {

    @SuppressLint("MissingPermission")
    fun enable(context: Context) {
        val zones = SafeZoneStore(context).getAll()
        if (zones.isEmpty()) {
            EventLogger.warn(context, "GEOFENCE", "No safe zones configured — nothing to register")
            return
        }

        val geofences = zones.mapNotNull { zone ->
            val transition = when {
                zone.alertOnEnter && zone.alertOnExit ->
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                zone.alertOnEnter -> Geofence.GEOFENCE_TRANSITION_ENTER
                zone.alertOnExit -> Geofence.GEOFENCE_TRANSITION_EXIT
                else -> return@mapNotNull null
            }
            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.lat, zone.lng, zone.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transition)
                .build()
        }

        if (geofences.isEmpty()) {
            EventLogger.warn(context, "GEOFENCE", "All zones have no transitions enabled")
            return
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        LocationServices.getGeofencingClient(context)
            .addGeofences(request, buildPendingIntent(context))
            .addOnSuccessListener {
                EventLogger.info(context, "GEOFENCE", "Registered ${geofences.size} zone(s)")
            }
            .addOnFailureListener { e ->
                EventLogger.error(context, "GEOFENCE", "Failed to register zones: ${e.message}")
            }
    }

    fun disable(context: Context) {
        LocationServices.getGeofencingClient(context)
            .removeGeofences(buildPendingIntent(context))
            .addOnSuccessListener {
                EventLogger.info(context, "GEOFENCE", "All geofences removed")
            }
            .addOnFailureListener { e ->
                EventLogger.warn(context, "GEOFENCE", "Failed to remove geofences: ${e.message}")
            }
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}
