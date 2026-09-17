package com.example.example.data.repository

import com.example.example.data.local.db.dao.OfflineAreaDao
import com.example.example.data.local.db.dao.PlaceOfInterestDao
import com.example.example.data.local.db.entity.OfflineAreaEntity
import com.example.example.data.local.db.mapper.toDomain
import com.example.example.data.local.db.mapper.toEntity
import com.example.example.data.offline.tiles.TileCacheDataSource
import com.example.example.data.remote.OpenTripMapApi
import com.example.example.data.remote.mapper.toDomain
import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.OfflineArea
import com.example.example.domain.repository.OfflineAreaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.max

private const val AVERAGE_TILE_SIZE_BYTES = 15_000L
private const val METERS_PER_DEGREE = 111_320.0

class OfflineAreaRepositoryImpl @Inject constructor(
    private val offlineAreaDao: OfflineAreaDao,
    private val placeOfInterestDao: PlaceOfInterestDao,
    private val tileCacheDataSource: TileCacheDataSource,
    private val api: OpenTripMapApi
) : OfflineAreaRepository {

    override fun observeOfflineAreas(): Flow<List<OfflineArea>> =
        offlineAreaDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun saveOfflineArea(
        name: String,
        boundingBox: GeoBoundingBox,
        minZoom: Int,
        maxZoom: Int
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Started)

        var failureMessage: String? = null
        tileCacheDataSource.downloadTiles(boundingBox, minZoom, maxZoom).collect { progress ->
            if (progress is DownloadProgress.Failed) {
                failureMessage = progress.message
            } else {
                emit(progress)
            }
        }

        if (failureMessage != null) {
            emit(DownloadProgress.Failed(failureMessage!!))
            return@flow
        }

        emit(DownloadProgress.PersistingPlaces)

        val centerLat = (boundingBox.north + boundingBox.south) / 2
        val centerLon = (boundingBox.east + boundingBox.west) / 2
        val radiusMeters = estimateRadiusMeters(boundingBox)

        val places = try {
            api.getPlaceInfo(lon = centerLon, lat = centerLat, radiusMeters = radiusMeters)
                .features.orEmpty().map { it.toDomain() }
        } catch (e: IOException) {
            emptyList()
        }

        val tileCount = tileCacheDataSource.estimateTileCount(boundingBox, minZoom, maxZoom)

        val areaId = offlineAreaDao.insert(
            OfflineAreaEntity(
                name = name,
                north = boundingBox.north,
                south = boundingBox.south,
                east = boundingBox.east,
                west = boundingBox.west,
                minZoom = minZoom,
                maxZoom = maxZoom,
                createdAtMillis = System.currentTimeMillis(),
                tileCount = tileCount,
                estimatedSizeBytes = tileCount * AVERAGE_TILE_SIZE_BYTES
            )
        )

        if (places.isNotEmpty()) {
            placeOfInterestDao.insertAll(places.map { it.toEntity(areaId) })
        }

        val savedArea = OfflineArea(
            id = areaId,
            name = name,
            boundingBox = boundingBox,
            minZoom = minZoom,
            maxZoom = maxZoom,
            createdAtMillis = System.currentTimeMillis(),
            tileCount = tileCount,
            estimatedSizeBytes = tileCount * AVERAGE_TILE_SIZE_BYTES
        )
        emit(DownloadProgress.Completed(savedArea))
    }

    override suspend fun deleteOfflineArea(id: Long): Result<Unit> = try {
        placeOfInterestDao.deleteByOfflineAreaId(id)
        offlineAreaDao.deleteById(id)
        // Shared osmdroid tile cache isn't partitioned per area; only safe to physically
        // clear it once no saved areas reference it anymore.
        if (offlineAreaDao.count() == 0) {
            tileCacheDataSource.purgeCache()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun estimateRadiusMeters(boundingBox: GeoBoundingBox): Int {
        val latSpanMeters = (boundingBox.north - boundingBox.south) * METERS_PER_DEGREE
        val avgLatRadians = Math.toRadians((boundingBox.north + boundingBox.south) / 2)
        val lonSpanMeters = (boundingBox.east - boundingBox.west) * METERS_PER_DEGREE * cos(avgLatRadians)
        return (max(latSpanMeters, lonSpanMeters) / 2).toInt().coerceIn(100, 50_000)
    }
}
