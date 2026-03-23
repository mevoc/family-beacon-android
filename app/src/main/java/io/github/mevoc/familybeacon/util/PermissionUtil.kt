package io.github.mevoc.familybeacon.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {

    const val REQ_SMS_LOC = 1001
    const val REQ_BATTERY = 1002
    const val REQ_PANIC = 1003
    const val REQ_GEOFENCE = 1004

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

    val PERMS_PANIC = arrayOf<String>()

    val PERMS_GEOFENCE = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
}