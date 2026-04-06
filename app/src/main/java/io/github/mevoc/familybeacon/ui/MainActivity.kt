package io.github.mevoc.familybeacon.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
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
    private lateinit var editBatteryThreshold: EditText

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

        // Initialize switch states (no auth needed for reading)
        swSms.isChecked = prefs.smsLocationEnabled
        swBattery.isChecked = prefs.batteryAlertEnabled
        swPanic.isChecked = prefs.panicEnabled
        swGeofence.isChecked = prefs.geofenceEnabled

        // Battery threshold input
        editBatteryThreshold = findViewById(R.id.editBatteryThreshold)
        editBatteryThreshold.setText(prefs.batteryAlertThreshold.toString())
        editBatteryThreshold.isEnabled = prefs.batteryAlertEnabled
        editBatteryThreshold.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toIntOrNull() ?: return
                if (v in 1..99) prefs.batteryAlertThreshold = v
            }
        })

        // Re-register battery receiver if feature was already enabled
        if (prefs.batteryAlertEnabled) {
            batteryReceiver = BatteryReceiver()
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.textVersion).text = versionName

        // Show warning and re-request if RECEIVE_SMS missing while SMS/Panic is enabled
        val permWarning = findViewById<TextView>(R.id.textPermWarning)
        val needsReceiveSms = prefs.smsLocationEnabled || prefs.panicEnabled
        val hasReceiveSms = PermissionUtil.hasAll(this, arrayOf(android.Manifest.permission.RECEIVE_SMS))
        if (needsReceiveSms && !hasReceiveSms) {
            permWarning.visibility = android.view.View.VISIBLE
            permWarning.setOnClickListener {
                PermissionUtil.request(this, arrayOf(android.Manifest.permission.RECEIVE_SMS), PermissionUtil.REQ_SMS_LOC)
            }
        } else {
            permWarning.visibility = android.view.View.GONE
        }

        // Request battery optimization exemption so SMS receiver works in background on Samsung etc.
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (needsReceiveSms && !pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
            })
        }

        val hasSms = PermissionUtil.hasAll(this, arrayOf(android.Manifest.permission.RECEIVE_SMS))
        val hasSendSms = PermissionUtil.hasAll(this, arrayOf(android.Manifest.permission.SEND_SMS))
        val hasLocation = PermissionUtil.hasAll(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION))
        EventLogger.info(this, "APP", "Permissions — RECEIVE_SMS:$hasSms SEND_SMS:$hasSendSms LOCATION:$hasLocation")
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
            auth.verifyUser { startActivity(Intent(this, WhitelistActivity::class.java)) }
        }

        findViewById<Button>(R.id.btnSafeZones).setOnClickListener {
            auth.verifyUser { startActivity(Intent(this, SafeZonesActivity::class.java)) }
        }

        btnEvents.setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }

        findViewById<Button>(R.id.btnPanicStop).setOnClickListener {
            stopService(Intent(this, PanicService::class.java))
            EventLogger.info(this, "PANIC", "Panic alarm stopped via button")
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

        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        val toggle = findViewById<Switch>(toggleId)

        // Geofence step 1: fine location granted → now ask for background location separately
        if (requestCode == PermissionUtil.REQ_GEOFENCE && granted) {
            val bgPerms = PermissionUtil.PERMS_GEOFENCE_BG
            if (bgPerms.isNotEmpty() && !PermissionUtil.hasAll(this, bgPerms)) {
                pendingToggleId = toggleId
                pendingDesiredState = desiredState
                PermissionUtil.request(this, bgPerms, PermissionUtil.REQ_GEOFENCE_BG)
                return
            }
            // No background perm needed (pre-Q) — fall through to apply
        }

        if (granted || requestCode == PermissionUtil.REQ_GEOFENCE_BG && granted) {
            when (toggleId) {
                R.id.switchSmsLocation -> {
                    prefs.smsLocationEnabled = desiredState
                    EventLogger.info(this, "PERMISSION", "Granted SMS+Location for POS feature")
                }
                R.id.switchGeofence -> {
                    prefs.geofenceEnabled = desiredState
                    if (desiredState) GeofenceHelper.enable(this) else GeofenceHelper.disable(this)
                    EventLogger.info(this, "PERMISSION", "Granted Location for Geofence")
                }
            }
            toggle.isChecked = desiredState
        } else {
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
                editBatteryThreshold.isEnabled = desiredState
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
