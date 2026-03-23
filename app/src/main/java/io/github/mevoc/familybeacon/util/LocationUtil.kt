package io.github.mevoc.familybeacon.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object LocationUtil {

    @SuppressLint("MissingPermission")
    fun requestBestEffortLocation(
        context: Context,
        onResult: (lat: Double, lon: Double, acc: Float?, timeMs: Long, source: String) -> Unit,
        onFailure: (reason: String) -> Unit
    ) {
        val client = LocationServices.getFusedLocationProviderClient(context)

        // 1) Försök current location (snabb om A-GPS/WiFi finns)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    LocationCache.save(context, loc.latitude, loc.longitude, loc.accuracy, loc.time)
                    onResult(loc.latitude, loc.longitude, loc.accuracy, loc.time, "current")
                } else {
                    // 2) fallback: last known from fused
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            LocationCache.save(context, last.latitude, last.longitude, last.accuracy, last.time)
                            onResult(last.latitude, last.longitude, last.accuracy, last.time, "lastLocation")
                        } else {
                            // 3) fallback: our own cache
                            val cached = LocationCache.get(context)
                            if (cached != null) {
                                onResult(cached.lat, cached.lon, cached.acc, cached.timeMs, "cache")
                            } else {
                                onFailure("no location available")
                            }
                        }
                    }.addOnFailureListener {
                        val cached = LocationCache.get(context)
                        if (cached != null) {
                            onResult(cached.lat, cached.lon, cached.acc, cached.timeMs, "cache")
                        } else {
                            onFailure("location failure")
                        }
                    }
                }
            }
            .addOnFailureListener {
                val cached = LocationCache.get(context)
                if (cached != null) {
                    onResult(cached.lat, cached.lon, cached.acc, cached.timeMs, "cache")
                } else {
                    onFailure("location failure")
                }
            }
    }
}