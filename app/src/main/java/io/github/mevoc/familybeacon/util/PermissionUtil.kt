package io.github.mevoc.familybeacon.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {

    const val REQ_SMS_LOC = 1001
    const val REQ_BATTERY = 1002
    const val REQ_PANIC = 1003
    const val REQ_GEOFENCE = 1004
    const val REQ_GEOFENCE_BG = 1005

    fun hasAll(context: Context, perms: Array<String>): Boolean =
        perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun request(activity: Activity, perms: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, perms, requestCode)
    }

    val PERMS_SMS_LOC = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val PERMS_BATTERY = arrayOf<String>()

    val PERMS_PANIC = arrayOf(Manifest.permission.RECEIVE_SMS)

    // Step 1: fine location only (background must be requested separately on Android 11+)
    val PERMS_GEOFENCE = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    // Step 2: background location, requested alone after fine location is granted
    val PERMS_GEOFENCE_BG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        arrayOf()
    }
}