package com.example.example

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    var place = MutableStateFlow<List<Feature>?>(null)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    fun getPlace(longitudeNow: Double, latittudeNow: Double){
        viewModelScope.launch {
            val placeInfo = RetrofitServices.searchPlacesInfo.getPlaceInfo(longitudeNow,latittudeNow)
            place.value = placeInfo.features
        }
    }


    fun initLocationClient(client: FusedLocationProviderClient) {
        fusedLocationClient = client
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                _locationFlow.value = locationResult.lastLocation
            }
        }

        locationCallback?.let { callback ->
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}



