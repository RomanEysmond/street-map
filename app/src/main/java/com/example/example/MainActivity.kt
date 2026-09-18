package com.example.example

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.example.databinding.ActivityMainBinding
import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.PlaceOfInterest
import com.example.example.offline.OfflineAreasActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var map: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val placeMarkers = mutableListOf<Marker>()

    private val viewModel: MainViewModel by viewModels()

    private val targetZoomLevel = 20.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        map = binding.osmmap
        map.setMultiTouchControls(true)

        initializeMap()
        initializeButtons()
        observeViewModel()
    }

    private fun initializeMap() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        myLocationOverlay.isDrawAccuracyEnabled = true
        map.overlays.add(myLocationOverlay)

        binding.myLocationButton.setOnClickListener {
            val myLocation = myLocationOverlay.myLocation
            if (myLocation != null) {
                map.controller.setCenter(myLocation)
                map.controller.animateTo(myLocation)
                map.controller.setZoom(targetZoomLevel)
            } else {
                Toast.makeText(this, "Location not available yet.", Toast.LENGTH_SHORT).show()
            }
        }

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
        if (!isLocationEnabled()) {
            showEnableLocationDialog()
        }
    }

    private fun initializeButtons() {
        binding.saveAreaButton.setOnClickListener { showSaveAreaDialog() }
        binding.viewOfflineAreasButton.setOnClickListener {
            startActivity(Intent(this, OfflineAreasActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderPlaces(state.nearbyPlaces)
                        state.errorMessage?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                launch {
                    viewModel.saveAreaState.collect { progress -> renderSaveAreaProgress(progress) }
                }
            }
        }
    }

    private fun renderPlaces(places: List<PlaceOfInterest>) {
        placeMarkers.forEach { map.overlays.remove(it) }
        placeMarkers.clear()

        places.forEach { place ->
            val marker = ZoomAwareMarker(map, minZoom = 16.0, maxZoom = 20.0).apply {
                position = GeoPoint(place.coordinates.latitude, place.coordinates.longitude)
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.dw)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = place.name
            }
            placeMarkers.add(marker)
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun renderSaveAreaProgress(progress: DownloadProgress?) {
        when (progress) {
            null -> {
                binding.saveAreaProgressBar.visibility = View.GONE
                binding.saveAreaStatusText.visibility = View.GONE
            }
            is DownloadProgress.Started -> {
                binding.saveAreaProgressBar.visibility = View.VISIBLE
                binding.saveAreaProgressBar.isIndeterminate = true
                binding.saveAreaStatusText.visibility = View.VISIBLE
                binding.saveAreaStatusText.text = getString(R.string.save_area_progress_started)
            }
            is DownloadProgress.TileProgress -> {
                binding.saveAreaProgressBar.visibility = View.VISIBLE
                binding.saveAreaProgressBar.isIndeterminate = false
                val total = progress.total.coerceAtLeast(1)
                binding.saveAreaProgressBar.progress = (progress.downloaded * 100 / total)
                binding.saveAreaStatusText.visibility = View.VISIBLE
                binding.saveAreaStatusText.text = getString(R.string.save_area_progress_tiles, progress.downloaded, progress.total)
            }
            is DownloadProgress.PersistingPlaces -> {
                binding.saveAreaProgressBar.visibility = View.VISIBLE
                binding.saveAreaProgressBar.isIndeterminate = true
                binding.saveAreaStatusText.visibility = View.VISIBLE
                binding.saveAreaStatusText.text = getString(R.string.save_area_progress_places)
            }
            is DownloadProgress.Completed -> {
                binding.saveAreaProgressBar.visibility = View.GONE
                binding.saveAreaStatusText.visibility = View.GONE
                Toast.makeText(this, getString(R.string.save_area_progress_completed, progress.area.name), Toast.LENGTH_SHORT).show()
                viewModel.clearSaveAreaState()
            }
            is DownloadProgress.Failed -> {
                binding.saveAreaProgressBar.visibility = View.GONE
                binding.saveAreaStatusText.visibility = View.GONE
                Toast.makeText(this, getString(R.string.save_area_progress_failed, progress.message), Toast.LENGTH_LONG).show()
                viewModel.clearSaveAreaState()
            }
        }
    }

    private fun showSaveAreaDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.save_area_dialog_name_hint)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.save_area_dialog_title)
            .setView(container)
            .setPositiveButton(R.string.save_area_dialog_positive) { _, _ ->
                val name = input.text.toString()
                val bbox = map.boundingBox
                val zoom = map.zoomLevelDouble.toInt()
                viewModel.saveCurrentArea(
                    name = name,
                    boundingBox = GeoBoundingBox(
                        north = bbox.latNorth,
                        south = bbox.latSouth,
                        east = bbox.lonEast,
                        west = bbox.lonWest
                    ),
                    minZoom = (zoom - 1).coerceAtLeast(1),
                    maxZoom = (zoom + 2)
                )
            }
            .setNegativeButton(R.string.save_area_dialog_negative, null)
            .show()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
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
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                myLocationOverlay.enableMyLocation()
                viewModel.retryLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
