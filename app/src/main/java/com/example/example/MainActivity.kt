package com.example.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Rect
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.example.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationListener
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay



class MainActivity : AppCompatActivity(), MapListener, LocationListener {
    private lateinit var map: MapView
    private lateinit var controller: IMapController
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private lateinit var locationManager: LocationManager
    private lateinit var binding: ActivityMainBinding
    private val targetZoomLevel = 20.0
    private val viewModel: MainViewModel by viewModels()
    companion object constants {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )
        map = binding.osmmap
        map.mapCenter
        map.setMultiTouchControls(true)
        map.getLocalVisibleRect(Rect())


        lifecycleScope.launch {
            initializeMap()
            delay(5000)
            viewModel.getPlace(
                mMyLocationOverlay.myLocation.longitude,
                mMyLocationOverlay.myLocation.latitude
            )
            viewModel.place.collect { streetName ->
                streetName?.forEach {

                    val longitudeMarker = it.geometry.coordinates.first()
                    val latitudeMarker = it.geometry.coordinates.last()
                    val startPoint1 = GeoPoint(latitudeMarker, longitudeMarker)
                    val startMarker1 = ZoomAwareMarker(binding.osmmap, minZoom = 6.0, maxZoom = 16.0).apply {
                        position = startPoint1
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.dw)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = it.properties.name

                    }


                    map.overlays.add(startMarker1)
                }
                Log.e("TAG", "============PLACE INFO in MAIN IS ${streetName}============")
            }
        }
    }

    private fun initializeMap() {
        mMyLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        controller = map.controller

        mMyLocationOverlay.enableMyLocation()
        mMyLocationOverlay.enableFollowLocation()
        mMyLocationOverlay.isDrawAccuracyEnabled = true


        val myLocationButton = binding.myLocationButton
        myLocationButton.setOnClickListener {
// Get the current location from the MyLocationNewOverlay
            val myLocation = mMyLocationOverlay.myLocation

            if (myLocation != null) {
// Move the map to the current location
                controller.setCenter(myLocation)
                controller.animateTo(myLocation)
                controller.setZoom(targetZoomLevel)
            } else {
                Toast.makeText(this, "Location not available yet.", Toast.LENGTH_SHORT).show()
            }
        }

        Log.e("TAG", "onCreate:in ${controller.zoomIn()}")
        Log.e("TAG", "onCreate: out ${controller.zoomOut()}")

        map.overlays.add(mMyLocationOverlay)
        map.addMapListener(this)

// Initialize LocationManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
// Request location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        }
            //Check enabled location
        if(!isLocationEnabled()){
            showEnableLocationDialog()
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, this)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Геолокация отключена")
            .setMessage("Пожалуйста, включите геолокацию для полного функционала")
            .setPositiveButton("Перейти в настройки") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Позже", null)
            .show()
    }


    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
// Permission granted, initialize the map
                    initializeMap()
                } else {
// Permission denied, handle accordingly (e.g., show a message to the user)
                    Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        Log.e("TAG", "onCreate:la ${event?.source?.mapCenter?.latitude}")
        Log.e("TAG", "onCreate:lo ${event?.source?.mapCenter?.longitude}")
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        Log.e("TAG", "onZoom zoom level: ${event?.zoomLevel} source: ${event?.source}")
        return false
    }

    override fun onLocationChanged(location: Location) {
// Handle location changes here
        Log.d("TAG", "Location changed: ${location.latitude}, ${location.longitude}")
    }
}

class ZoomAwareMarker(
    mapView: MapView,
    private val minZoom: Double = 10.0,
    private val maxZoom: Double = 20.0
) : Marker(mapView) {

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val currentZoom = mapView.zoomLevelDouble
        if (currentZoom in minZoom..maxZoom) {
            super.draw(canvas, mapView, shadow)
        }
    }
}


private fun LocationManager.requestLocationUpdates(
    gpsProvider: String,
    i: Int,
    fl: Float,
    mainActivity: MainActivity
) {

}