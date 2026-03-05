package io.github.mevoc.familybeacon.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.mevoc.familybeacon.R
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.geofence.GeofenceHelper
import io.github.mevoc.familybeacon.service.PanicService
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var auth: AuthHelper
    private lateinit var prefs: FeaturePrefs

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
            // Placeholder for now
            textStatus.text = "Status:\n• Whitelist screen not implemented yet"
        }

        btnEvents.setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }
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
                setState(desiredState)
                toggle.post {
                    toggle.setOnCheckedChangeListener(null)
                    toggle.isChecked = desiredState
                    toggle.setOnCheckedChangeListener { _, _ -> } // keep stable
                    // Rewire properly after setting
                    wireToggle(toggle, getState, setState)
                }
                EventLogger.info(this, "TOGGLE", "${toggle.text} -> $desiredState")
                if (toggle.id == R.id.switchGeofence) {
                    if (desiredState) GeofenceHelper.enable(this) else GeofenceHelper.disable(this)
                }
                //if (toggle.id == R.id.switchPanic) {
                //    if (desiredState) PanicService.enable(this) else PanicService.disable(this)
                //}
            }

            // If auth fails/cancelled, restore wiring
            toggle.post {
                wireToggle(toggle, getState, setState)
            }
        }
    }
}