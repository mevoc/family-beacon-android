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
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.LocationUtil

object GeofenceHelper {

    private const val FENCE_ID = "HOME_ZONE"
    private const val RADIUS_METERS = 200f

    @SuppressLint("MissingPermission")
    fun enable(context: Context) {
        LocationUtil.requestBestEffortLocation(
            context = context,
            onResult = { lat, lon, _, _, source ->
                val prefs = FeaturePrefs(context)
                prefs.geofenceLat = lat.toFloat()
                prefs.geofenceLng = lon.toFloat()

                val geofence = Geofence.Builder()
                    .setRequestId(FENCE_ID)
                    .setCircularRegion(lat, lon, RADIUS_METERS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER
                    )
                    .build()

                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()

                LocationServices.getGeofencingClient(context)
                    .addGeofences(request, buildPendingIntent(context))
                    .addOnSuccessListener {
                        EventLogger.info(context, "GEOFENCE", "Geofence registered at $lat,$lon (src: $source, r: ${RADIUS_METERS}m)")
                    }
                    .addOnFailureListener { e ->
                        EventLogger.error(context, "GEOFENCE", "Failed to register geofence: ${e.message}")
                    }
            },
            onFailure = { reason ->
                EventLogger.error(context, "GEOFENCE", "Could not get location to set geofence: $reason")
            }
        )
    }

    fun disable(context: Context) {
        LocationServices.getGeofencingClient(context)
            .removeGeofences(buildPendingIntent(context))
            .addOnSuccessListener {
                EventLogger.info(context, "GEOFENCE", "Geofence removed")
            }
            .addOnFailureListener { e ->
                EventLogger.warn(context, "GEOFENCE", "Failed to remove geofence: ${e.message}")
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
