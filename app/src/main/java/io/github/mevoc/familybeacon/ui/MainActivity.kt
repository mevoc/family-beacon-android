package io.github.mevoc.familybeacon.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import io.github.mevoc.familybeacon.R
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.geofence.GeofenceHelper
import io.github.mevoc.familybeacon.receiver.BatteryReceiver
import io.github.mevoc.familybeacon.service.PanicService
import io.github.mevoc.familybeacon.util.AuthHelper
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.PermissionUtil
import io.github.mevoc.familybeacon.util.Prefs


class MainActivity : AppCompatActivity() {

    private lateinit var auth: AuthHelper
    private lateinit var prefs: FeaturePrefs

    private var pendingToggleId: Int? = null
    private var pendingDesiredState: Boolean = false

    private var batteryReceiver: BatteryReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safety: require consent first
        if (!Prefs.isConsentGiven(this)) {
            startActivity(Intent(this, ConsentActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        auth = AuthHelper(this).apply {
            requireLockForFeature = true // fallback 1: guide to enable lock
        }
        prefs = FeaturePrefs(this)

        val swSms = findViewById<Switch>(R.id.switchSmsLocation)
        val swBattery = findViewById<Switch>(R.id.switchBattery)
        val swPanic = findViewById<Switch>(R.id.switchPanic)
        val swGeofence = findViewById<Switch>(R.id.switchGeofence)

        val btnWhitelist = findViewById<Button>(R.id.btnWhitelist)
        val btnEvents = findViewById<Button>(R.id.btnEvents)
        val textStatus = findViewById<TextView>(R.id.textStatus)

        // Initialize switch states (no auth needed for reading)
        swSms.isChecked = prefs.smsLocationEnabled
        swBattery.isChecked = prefs.batteryAlertEnabled
        swPanic.isChecked = prefs.panicEnabled
        swGeofence.isChecked = prefs.geofenceEnabled

        // Re-register battery receiver if feature was already enabled
        if (prefs.batteryAlertEnabled) {
            batteryReceiver = BatteryReceiver()
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }

        EventLogger.info(this, "APP", "Main screen opened")

        // Wire toggles with auth
        wireToggle(swSms,
            getState = { prefs.smsLocationEnabled },
            setState = { prefs.smsLocationEnabled = it }
        )

        wireToggle(swBattery,
            getState = { prefs.batteryAlertEnabled },
            setState = { prefs.batteryAlertEnabled = it }
        )

        wireToggle(swPanic,
            getState = { prefs.panicEnabled },
            setState = { prefs.panicEnabled = it }
        )

        wireToggle(swGeofence,
            getState = { prefs.geofenceEnabled },
            setState = { prefs.geofenceEnabled = it }
        )

        btnWhitelist.setOnClickListener {
            startActivity(Intent(this, WhitelistActivity::class.java))
        }

        btnEvents.setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val toggleId = pendingToggleId ?: return
        val desiredState = pendingDesiredState
        pendingToggleId = null

        val granted = grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
        val toggle = findViewById<Switch>(toggleId)

        if (granted) {
            // Apply state and log
            when (toggleId) {
                R.id.switchSmsLocation -> {
                    prefs.smsLocationEnabled = desiredState
                    EventLogger.info(this, "PERMISSION", "Granted SMS+Location for LOC feature")
                }
                R.id.switchGeofence -> {
                    prefs.geofenceEnabled = desiredState
                    if (desiredState) GeofenceHelper.enable(this) else GeofenceHelper.disable(this)
                    EventLogger.info(this, "PERMISSION", "Granted Location for Geofence")
                }
            }
            toggle.isChecked = desiredState
        } else {
            // Permission denied → keep OFF
            when (toggleId) {
                R.id.switchSmsLocation -> prefs.smsLocationEnabled = false
                R.id.switchGeofence -> prefs.geofenceEnabled = false
            }
            toggle.isChecked = false
            EventLogger.warn(this, "PERMISSION", "Denied permissions for toggleId=$toggleId")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let { unregisterReceiver(it) }
        batteryReceiver = null
    }

    private fun applyFeatureToggle(toggle: Switch, desiredState: Boolean, setState: (Boolean) -> Unit) {
        setState(desiredState)

        when (toggle.id) {
            R.id.switchGeofence -> {
                if (desiredState) GeofenceHelper.enable(this) else GeofenceHelper.disable(this)
            }
            R.id.switchBattery -> {
                if (desiredState) {
                    if (batteryReceiver == null) {
                        batteryReceiver = BatteryReceiver()
                        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    }
                } else {
                    batteryReceiver?.let { unregisterReceiver(it) }
                    batteryReceiver = null
                }
            }
            R.id.switchPanic -> {
                if (!desiredState) {
                    stopService(Intent(this, PanicService::class.java))
                }
            }
            // SMS receiver is triggered by OS, no explicit start needed
        }

        EventLogger.info(this, "TOGGLE", "${toggle.text} -> $desiredState")
    }

    private fun wireToggle(
        toggle: Switch,
        getState: () -> Boolean,
        setState: (Boolean) -> Unit
    ) {
        toggle.setOnCheckedChangeListener(null)
        toggle.isChecked = getState()

        toggle.setOnCheckedChangeListener { _, desiredState ->
            // Immediately revert UI; apply only after auth
            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = getState()
            toggle.setOnCheckedChangeListener { _, _ -> } // block re-entrancy

            auth.verifyUser {
                // Auth OK → permissions gate
                val result: Pair<Array<String>, Int> = when (toggle.id) {
                    R.id.switchSmsLocation -> PermissionUtil.PERMS_SMS_LOC to PermissionUtil.REQ_SMS_LOC
                    R.id.switchGeofence -> PermissionUtil.PERMS_GEOFENCE to PermissionUtil.REQ_GEOFENCE
                    R.id.switchBattery -> PermissionUtil.PERMS_BATTERY to PermissionUtil.REQ_BATTERY
                    R.id.switchPanic -> PermissionUtil.PERMS_PANIC to PermissionUtil.REQ_PANIC
                    else -> emptyArray<String>() to 0
                }
                val (perms, reqCode) = result

                if (perms.isEmpty() || PermissionUtil.hasAll(this, perms)) {
                    // Permissions OK → apply state + side effects
                    applyFeatureToggle(toggle, desiredState, setState)
                    // update UI
                    toggle.post {
                        toggle.setOnCheckedChangeListener(null)
                        toggle.isChecked = desiredState
                        wireToggle(toggle, getState, setState)
                    }
                } else {
                    // Need permissions
                    pendingToggleId = toggle.id
                    pendingDesiredState = desiredState
                    PermissionUtil.request(this, perms, reqCode)
                    // UI stays OFF until result
                    toggle.post { wireToggle(toggle, getState, setState) }
                }
            }
        }
    }
}
