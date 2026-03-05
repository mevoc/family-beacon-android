package io.github.mevoc.familybeacon.geofence

import android.content.Context
import io.github.mevoc.familybeacon.data.EventLogger

object GeofenceHelper {
    fun enable(context: Context) {
        EventLogger.info(context, "GEOFENCE", "Geofence enabled (stub)")
    }

    fun disable(context: Context) {
        EventLogger.info(context, "GEOFENCE", "Geofence disabled (stub)")
    }
}