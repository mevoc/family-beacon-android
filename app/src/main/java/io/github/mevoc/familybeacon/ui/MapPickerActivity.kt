package io.github.mevoc.familybeacon.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.github.mevoc.familybeacon.R
import io.github.mevoc.familybeacon.util.LocationUtil

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var confirmButton: Button
    private var selectedLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        confirmButton = findViewById<Button>(R.id.btnMapConfirm).apply {
            isEnabled = false
            setOnClickListener {
                val pos = selectedLatLng ?: return@setOnClickListener
                setResult(RESULT_OK, Intent().apply {
                    putExtra("lat", pos.latitude)
                    putExtra("lng", pos.longitude)
                })
                finish()
            }
        }

        findViewById<Button>(R.id.btnMapCancel).setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            map.clear()
            map.addMarker(MarkerOptions().position(latLng))
            confirmButton.isEnabled = true
        }

        // Centre on current location if available
        LocationUtil.requestBestEffortLocation(
            context = this,
            onResult = { lat, lon, _, _, _ ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 13f))
            },
            onFailure = { /* keep default map centre */ }
        )
    }
}
