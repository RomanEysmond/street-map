package com.example.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.example.domain.model.Coordinates
import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.PlaceOfInterest
import com.example.example.domain.repository.LocationRepository
import com.example.example.domain.repository.PlacesRepository
import com.example.example.domain.usecase.SaveOfflineAreaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class MainUiState(
    val userLocation: Coordinates? = null,
    val nearbyPlaces: List<PlaceOfInterest> = emptyList(),
    val isLoadingPlaces: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val placesRepository: PlacesRepository,
    private val saveOfflineAreaUseCase: SaveOfflineAreaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _saveAreaState = MutableStateFlow<DownloadProgress?>(null)
    val saveAreaState: StateFlow<DownloadProgress?> = _saveAreaState.asStateFlow()

    init {
        observeLocation()
    }

    /**
     * Location permission may not be granted yet when this ViewModel is first created, in which
     * case [LocationRepository.observeLocationUpdates] fails immediately and its flow terminates
     * for good. Call this again once permission has just been granted to resume location updates
     * without needing to recreate the ViewModel.
     */
    fun retryLocationUpdates() {
        observeLocation()
    }

    @OptIn(FlowPreview::class)
    private fun observeLocation() {
        locationRepository.observeLocationUpdates()
            .map { it.coordinates }
            .distinctUntilChanged()
            .debounce(LOCATION_DEBOUNCE_MILLIS)
            .onEach { coordinates -> refreshNearbyPlaces(coordinates) }
            .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshNearbyPlaces(coordinates: Coordinates) {
        _uiState.update { it.copy(userLocation = coordinates, isLoadingPlaces = true, errorMessage = null) }
        placesRepository.getNearbyPlaces(coordinates)
            .onSuccess { places -> _uiState.update { it.copy(nearbyPlaces = places, isLoadingPlaces = false) } }
            .onFailure { e -> _uiState.update { it.copy(isLoadingPlaces = false, errorMessage = e.message) } }
    }

    fun saveCurrentArea(name: String, boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int) {
        saveOfflineAreaUseCase(name, boundingBox, minZoom, maxZoom)
            .onEach { progress -> _saveAreaState.value = progress }
            .catch { e -> _saveAreaState.value = DownloadProgress.Failed(e.message ?: "Unknown error") }
            .launchIn(viewModelScope)
    }

    fun clearSaveAreaState() {
        _saveAreaState.value = null
    }

    private companion object {
        const val LOCATION_DEBOUNCE_MILLIS = 3_000L
    }
}
