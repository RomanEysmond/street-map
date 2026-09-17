package com.example.example

import com.example.example.domain.model.Coordinates
import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.OfflineArea
import com.example.example.domain.model.PlaceOfInterest
import com.example.example.domain.model.UserLocation
import com.example.example.domain.repository.LocationRepository
import com.example.example.domain.repository.OfflineAreaRepository
import com.example.example.domain.repository.PlacesRepository
import com.example.example.domain.usecase.SaveOfflineAreaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeLocationRepository(private val failFirstCall: Boolean = false) : LocationRepository {
    private val locations = MutableSharedFlow<UserLocation>(extraBufferCapacity = 1)
    private var callCount = 0

    override fun observeLocationUpdates(): Flow<UserLocation> {
        callCount++
        return if (failFirstCall && callCount == 1) {
            flow { throw SecurityException("Location permission not granted") }
        } else {
            locations.asSharedFlow()
        }
    }

    suspend fun emit(location: UserLocation) = locations.emit(location)
}

private class FakePlacesRepository : PlacesRepository {
    var callCount = 0
        private set

    override suspend fun getNearbyPlaces(coordinates: Coordinates, radiusMeters: Int): Result<List<PlaceOfInterest>> {
        callCount++
        return Result.success(emptyList())
    }
}

private class FakeOfflineAreaRepository : OfflineAreaRepository {
    var lastSavedName: String? = null

    override fun observeOfflineAreas(): Flow<List<OfflineArea>> = flowOf(emptyList())

    override fun saveOfflineArea(name: String, boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Flow<DownloadProgress> {
        lastSavedName = name
        return flowOf(
            DownloadProgress.Completed(
                OfflineArea(
                    id = 1, name = name, boundingBox = boundingBox,
                    minZoom = minZoom, maxZoom = maxZoom, createdAtMillis = 0L,
                    tileCount = 1, estimatedSizeBytes = 1
                )
            )
        )
    }

    override suspend fun deleteOfflineArea(id: Long): Result<Unit> = Result.success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `single location emission triggers exactly one places lookup`() = runTest {
        val locationRepository = FakeLocationRepository()
        val placesRepository = FakePlacesRepository()
        val viewModel = MainViewModel(
            locationRepository = locationRepository,
            placesRepository = placesRepository,
            saveOfflineAreaUseCase = SaveOfflineAreaUseCase(FakeOfflineAreaRepository())
        )

        locationRepository.emit(UserLocation(Coordinates(1.0, 2.0), accuracyMeters = null, timestampMillis = 0L))
        advanceUntilIdle()

        assertEquals(1, placesRepository.callCount)
        assertEquals(Coordinates(1.0, 2.0), viewModel.uiState.value.userLocation)
    }

    @Test
    fun `retryLocationUpdates resumes location flow after an initial permission failure`() = runTest {
        val locationRepository = FakeLocationRepository(failFirstCall = true)
        val placesRepository = FakePlacesRepository()
        val viewModel = MainViewModel(
            locationRepository = locationRepository,
            placesRepository = placesRepository,
            saveOfflineAreaUseCase = SaveOfflineAreaUseCase(FakeOfflineAreaRepository())
        )
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.retryLocationUpdates()
        locationRepository.emit(UserLocation(Coordinates(3.0, 4.0), accuracyMeters = null, timestampMillis = 0L))
        advanceUntilIdle()

        assertEquals(1, placesRepository.callCount)
        assertEquals(Coordinates(3.0, 4.0), viewModel.uiState.value.userLocation)
    }

    @Test
    fun `saveCurrentArea delegates to the use case and publishes progress`() = runTest {
        val offlineAreaRepository = FakeOfflineAreaRepository()
        val viewModel = MainViewModel(
            locationRepository = FakeLocationRepository(),
            placesRepository = FakePlacesRepository(),
            saveOfflineAreaUseCase = SaveOfflineAreaUseCase(offlineAreaRepository)
        )
        val boundingBox = GeoBoundingBox(north = 10.1, south = 10.0, east = 20.1, west = 20.0)

        assertNull(viewModel.saveAreaState.value)
        viewModel.saveCurrentArea("Downtown", boundingBox, minZoom = 14, maxZoom = 16)
        advanceUntilIdle()

        assertEquals("Downtown", offlineAreaRepository.lastSavedName)
        assertTrue(viewModel.saveAreaState.value is DownloadProgress.Completed)
    }

    @Test
    fun `saveCurrentArea with a blank name is rejected without touching the repository`() = runTest {
        val offlineAreaRepository = FakeOfflineAreaRepository()
        val viewModel = MainViewModel(
            locationRepository = FakeLocationRepository(),
            placesRepository = FakePlacesRepository(),
            saveOfflineAreaUseCase = SaveOfflineAreaUseCase(offlineAreaRepository)
        )
        val boundingBox = GeoBoundingBox(north = 10.1, south = 10.0, east = 20.1, west = 20.0)

        viewModel.saveCurrentArea("   ", boundingBox, minZoom = 14, maxZoom = 16)
        advanceUntilIdle()

        assertEquals(null, offlineAreaRepository.lastSavedName)
        assertTrue(viewModel.saveAreaState.value is DownloadProgress.Failed)
    }

    @Test
    fun `clearSaveAreaState resets the state back to null`() = runTest {
        val offlineAreaRepository = FakeOfflineAreaRepository()
        val viewModel = MainViewModel(
            locationRepository = FakeLocationRepository(),
            placesRepository = FakePlacesRepository(),
            saveOfflineAreaUseCase = SaveOfflineAreaUseCase(offlineAreaRepository)
        )
        val boundingBox = GeoBoundingBox(north = 10.1, south = 10.0, east = 20.1, west = 20.0)
        viewModel.saveCurrentArea("Downtown", boundingBox, minZoom = 14, maxZoom = 16)
        advanceUntilIdle()
        assertNotNull(viewModel.saveAreaState.value)

        viewModel.clearSaveAreaState()

        assertNull(viewModel.saveAreaState.value)
    }
}
