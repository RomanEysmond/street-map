package com.example.example.data.repository

import com.example.example.data.connectivity.ConnectivityChecker
import com.example.example.data.local.db.dao.PlaceOfInterestDao
import com.example.example.data.local.db.entity.PlaceOfInterestEntity
import com.example.example.data.remote.OpenTripMapApi
import com.example.example.data.remote.dto.PlacesResponseDto
import com.example.example.domain.model.Coordinates
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeOpenTripMapApi(
    private val response: PlacesResponseDto? = null,
    private val error: Throwable? = null
) : OpenTripMapApi {
    override suspend fun getPlaceInfo(lon: Double, lat: Double, radiusMeters: Int): PlacesResponseDto {
        error?.let { throw it }
        return response ?: PlacesResponseDto(features = emptyList(), type = "FeatureCollection")
    }
}

private class FakePlaceOfInterestDao(
    private val cached: List<PlaceOfInterestEntity> = emptyList()
) : PlaceOfInterestDao {
    override suspend fun insertAll(places: List<PlaceOfInterestEntity>) = Unit
    override suspend fun getByOfflineAreaId(offlineAreaId: Long): List<PlaceOfInterestEntity> = emptyList()
    override suspend fun deleteByOfflineAreaId(offlineAreaId: Long) = Unit
    override suspend fun getWithinBoundingBox(north: Double, south: Double, east: Double, west: Double) = cached
}

private class FakeConnectivityChecker(private val online: Boolean) : ConnectivityChecker {
    override fun isOnline(): Boolean = online
}

class PlacesRepositoryImplTest {

    private val coordinates = Coordinates(latitude = 59.9, longitude = 30.3)
    private val cachedEntity = PlaceOfInterestEntity(
        xid = "xid1", offlineAreaId = 1, name = "Cached place",
        latitude = 59.9, longitude = 30.3, kinds = null,
        distanceMeters = null, rating = null, wikidataId = null, osmId = null
    )

    @Test
    fun `falls back to cached places when offline`() = runTest {
        val repository = PlacesRepositoryImpl(
            api = FakeOpenTripMapApi(),
            placeOfInterestDao = FakePlaceOfInterestDao(cached = listOf(cachedEntity)),
            connectivityChecker = FakeConnectivityChecker(online = false)
        )

        val result = repository.getNearbyPlaces(coordinates)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.single()?.id == "xid1")
    }

    @Test
    fun `falls back to cache when the network call throws`() = runTest {
        val repository = PlacesRepositoryImpl(
            api = FakeOpenTripMapApi(error = IOException("no network")),
            placeOfInterestDao = FakePlaceOfInterestDao(cached = emptyList()),
            connectivityChecker = FakeConnectivityChecker(online = true)
        )

        val result = repository.getNearbyPlaces(coordinates)

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns network result when online`() = runTest {
        val repository = PlacesRepositoryImpl(
            api = FakeOpenTripMapApi(response = PlacesResponseDto(features = emptyList(), type = "FeatureCollection")),
            placeOfInterestDao = FakePlaceOfInterestDao(),
            connectivityChecker = FakeConnectivityChecker(online = true)
        )

        val result = repository.getNearbyPlaces(coordinates)

        assertTrue(result.isSuccess)
    }
}
