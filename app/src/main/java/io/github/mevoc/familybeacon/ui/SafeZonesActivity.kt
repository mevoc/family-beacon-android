package io.github.mevoc.familybeacon.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import io.github.mevoc.familybeacon.R
import io.github.mevoc.familybeacon.data.EventLogger
import io.github.mevoc.familybeacon.geofence.GeofenceHelper
import io.github.mevoc.familybeacon.util.FeaturePrefs
import io.github.mevoc.familybeacon.util.LocationUtil
import io.github.mevoc.familybeacon.util.SafeZone
import io.github.mevoc.familybeacon.util.SafeZoneStore

class SafeZonesActivity : AppCompatActivity() {

    private lateinit var store: SafeZoneStore
    private lateinit var container: LinearLayout
    private lateinit var editLat: EditText
    private lateinit var editLng: EditText

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", Double.NaN) ?: return@registerForActivityResult
            val lng = result.data?.getDoubleExtra("lng", Double.NaN) ?: return@registerForActivityResult
            if (!lat.isNaN() && !lng.isNaN()) {
                editLat.setText("%.6f".format(lat))
                editLng.setText("%.6f".format(lng))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_zones)

        store = SafeZoneStore(this)
        container = findViewById(R.id.zonesContainer)
        editLat = findViewById(R.id.editLat)
        editLng = findViewById(R.id.editLng)

        val editName = findViewById<EditText>(R.id.editZoneName)
        val editRadius = findViewById<EditText>(R.id.editRadius)
        val cbAddEnter = findViewById<CheckBox>(R.id.cbAddEnter)
        val cbAddExit = findViewById<CheckBox>(R.id.cbAddExit)

        editRadius.setText("200")

        findViewById<Button>(R.id.btnCurrentPos).setOnClickListener {
            LocationUtil.requestBestEffortLocation(
                context = this,
                onResult = { lat, lon, _, _, _ ->
                    editLat.setText("%.6f".format(lat))
                    editLng.setText("%.6f".format(lon))
                },
                onFailure = { reason ->
                    Toast.makeText(this, "Could not get location: $reason", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<Button>(R.id.btnPickMap).setOnClickListener {
            mapPickerLauncher.launch(Intent(this, MapPickerActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddZone).setOnClickListener {
            val name = editName.text?.toString()?.trim().orEmpty()
            val lat = editLat.text?.toString()?.toDoubleOrNull()
            val lng = editLng.text?.toString()?.toDoubleOrNull()
            val radius = editRadius.text?.toString()?.toFloatOrNull() ?: 200f

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a zone name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lat == null || lng == null) {
                Toast.makeText(this, "Enter valid coordinates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            store.add(SafeZone(
                name = name,
                lat = lat,
                lng = lng,
                radiusMeters = radius.coerceIn(10f, 50_000f),
                alertOnEnter = cbAddEnter.isChecked,
                alertOnExit = cbAddExit.isChecked
            ))
            EventLogger.info(this, "GEOFENCE", "Added zone \"$name\" at $lat,$lng (r=${radius.toInt()}m)")

            editName.setText("")
            editLat.setText("")
            editLng.setText("")
            editRadius.setText("200")
            cbAddEnter.isChecked = true
            cbAddExit.isChecked = true

            refreshGeofencesIfEnabled()
            refreshList()
        }

        refreshList()
    }

    private fun refreshList() {
        container.removeAllViews()
        val zones = store.getAll()
        if (zones.isEmpty()) {
            container.addView(TextView(this).apply {
                text = getString(R.string.safe_zone_empty)
                textSize = 14f
            })
        } else {
            zones.forEach { renderZone(it) }
        }
    }

    private fun renderZone(zone: SafeZone) {
        val row = layoutInflater.inflate(R.layout.item_safe_zone, container, false)

        row.findViewById<TextView>(R.id.zoneName).text = zone.name
        row.findViewById<TextView>(R.id.zoneDetails).text =
            "%.5f, %.5f  •  %dm".format(zone.lat, zone.lng, zone.radiusMeters.toInt())

        val cbEnter = row.findViewById<CheckBox>(R.id.cbEnter)
        val cbExit = row.findViewById<CheckBox>(R.id.cbExit)
        cbEnter.isChecked = zone.alertOnEnter
        cbExit.isChecked = zone.alertOnExit

        fun saveUpdate() {
            store.update(zone.copy(alertOnEnter = cbEnter.isChecked, alertOnExit = cbExit.isChecked))
            refreshGeofencesIfEnabled()
        }

        cbEnter.setOnCheckedChangeListener { _, _ -> saveUpdate() }
        cbExit.setOnCheckedChangeListener { _, _ -> saveUpdate() }

        row.findViewById<Button>(R.id.btnRemoveZone).setOnClickListener {
            store.remove(zone.id)
            EventLogger.info(this, "GEOFENCE", "Removed zone \"${zone.name}\"")
            refreshGeofencesIfEnabled()
            refreshList()
        }

        container.addView(row)
    }

    private fun refreshGeofencesIfEnabled() {
        if (FeaturePrefs(this).geofenceEnabled) {
            GeofenceHelper.disable(this)
            GeofenceHelper.enable(this)
        }
    }
}
