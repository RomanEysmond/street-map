package com.example.example.data.repository

import com.example.example.data.connectivity.ConnectivityChecker
import com.example.example.data.local.db.dao.PlaceOfInterestDao
import com.example.example.data.local.db.mapper.toDomain
import com.example.example.data.remote.OpenTripMapApi
import com.example.example.data.remote.mapper.toDomain
import com.example.example.domain.model.Coordinates
import com.example.example.domain.model.PlaceOfInterest
import com.example.example.domain.repository.PlacesRepository
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private const val EARTH_RADIUS_METERS = 6_371_000.0

class PlacesRepositoryImpl @Inject constructor(
    private val api: OpenTripMapApi,
    private val placeOfInterestDao: PlaceOfInterestDao,
    private val connectivityChecker: ConnectivityChecker
) : PlacesRepository {

    override suspend fun getNearbyPlaces(coordinates: Coordinates, radiusMeters: Int): Result<List<PlaceOfInterest>> {
        if (connectivityChecker.isOnline()) {
            return try {
                val response = api.getPlaceInfo(lon = coordinates.longitude, lat = coordinates.latitude, radiusMeters = radiusMeters)
                Result.success(response.features.orEmpty().map { it.toDomain() })
            } catch (e: IOException) {
                getCachedNearbyPlaces(coordinates, radiusMeters)
            }
        }
        return getCachedNearbyPlaces(coordinates, radiusMeters)
    }

    private suspend fun getCachedNearbyPlaces(coordinates: Coordinates, radiusMeters: Int): Result<List<PlaceOfInterest>> {
        val degreeSpan = radiusMeters / EARTH_RADIUS_METERS * (180.0 / Math.PI)
        val cached = placeOfInterestDao.getWithinBoundingBox(
            north = min(coordinates.latitude + degreeSpan, 90.0),
            south = max(coordinates.latitude - degreeSpan, -90.0),
            east = min(coordinates.longitude + degreeSpan, 180.0),
            west = max(coordinates.longitude - degreeSpan, -180.0)
        )
        return if (cached.isEmpty()) {
            Result.failure(IOException("No offline data available for this location"))
        } else {
            Result.success(cached.map { it.toDomain() })
        }
    }
}
