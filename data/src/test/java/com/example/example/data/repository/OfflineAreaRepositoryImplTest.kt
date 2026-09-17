package com.example.example.data.repository

import com.example.example.data.local.db.dao.OfflineAreaDao
import com.example.example.data.local.db.dao.PlaceOfInterestDao
import com.example.example.data.local.db.entity.OfflineAreaEntity
import com.example.example.data.local.db.entity.PlaceOfInterestEntity
import com.example.example.data.offline.tiles.TileCacheDataSource
import com.example.example.data.remote.OpenTripMapApi
import com.example.example.data.remote.dto.FeatureDto
import com.example.example.data.remote.dto.GeometryDto
import com.example.example.data.remote.dto.PlacesResponseDto
import com.example.example.data.remote.dto.PropertiesDto
import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeOfflineAreaDao : OfflineAreaDao {
    private val areas = MutableStateFlow<List<OfflineAreaEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<OfflineAreaEntity>> = areas

    override suspend fun count(): Int = areas.value.size

    override suspend fun insert(area: OfflineAreaEntity): Long {
        val id = nextId++
        areas.value = areas.value + area.copy(id = id)
        return id
    }

    override suspend fun deleteById(id: Long) {
        areas.value = areas.value.filterNot { it.id == id }
    }
}

private class RecordingPlaceOfInterestDao : PlaceOfInterestDao {
    val insertedBatches = mutableListOf<List<PlaceOfInterestEntity>>()
    val deletedOfflineAreaIds = mutableListOf<Long>()

    override suspend fun insertAll(places: List<PlaceOfInterestEntity>) {
        insertedBatches.add(places)
    }

    override suspend fun getByOfflineAreaId(offlineAreaId: Long): List<PlaceOfInterestEntity> = emptyList()

    override suspend fun deleteByOfflineAreaId(offlineAreaId: Long) {
        deletedOfflineAreaIds.add(offlineAreaId)
    }

    override suspend fun getWithinBoundingBox(north: Double, south: Double, east: Double, west: Double): List<PlaceOfInterestEntity> =
        emptyList()
}

private class FakeTileCacheDataSource(
    private val downloadResult: List<DownloadProgress> = listOf(DownloadProgress.TileProgress(1, 1)),
    private val tileCount: Int = 4
) : TileCacheDataSource {
    var purgeCalled = false
        private set

    override fun estimateTileCount(boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Int = tileCount

    override fun downloadTiles(boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Flow<DownloadProgress> =
        downloadResult.asFlow()

    override fun purgeCache(): Boolean {
        purgeCalled = true
        return true
    }
}

private class StubOpenTripMapApi(
    private val response: PlacesResponseDto = PlacesResponseDto(features = emptyList(), type = "FeatureCollection")
) : OpenTripMapApi {
    override suspend fun getPlaceInfo(lon: Double, lat: Double, radiusMeters: Int): PlacesResponseDto = response
}

private val testBoundingBox = GeoBoundingBox(north = 10.1, south = 10.0, east = 20.1, west = 20.0)

class OfflineAreaRepositoryImplTest {

    @Test
    fun `successful save persists area and places and emits Completed`() = runTest {
        val offlineAreaDao = FakeOfflineAreaDao()
        val placeOfInterestDao = RecordingPlaceOfInterestDao()
        val featureDto = FeatureDto(
            geometry = GeometryDto(coordinates = listOf(20.05, 10.05), type = "Point"),
            id = "node/1",
            properties = PropertiesDto(dist = 10.0, name = "Cafe", rate = 1, xid = "xid1"),
            type = "Feature"
        )
        val repository = OfflineAreaRepositoryImpl(
            offlineAreaDao = offlineAreaDao,
            placeOfInterestDao = placeOfInterestDao,
            tileCacheDataSource = FakeTileCacheDataSource(),
            api = StubOpenTripMapApi(PlacesResponseDto(features = listOf(featureDto), type = "FeatureCollection"))
        )

        val progressEvents = repository.saveOfflineArea("My area", testBoundingBox, 14, 16).toList()

        assertTrue(progressEvents.first() is DownloadProgress.Started)
        val completed = progressEvents.last() as DownloadProgress.Completed
        assertEquals("My area", completed.area.name)
        assertEquals(1, offlineAreaDao.count())
        assertEquals(1, placeOfInterestDao.insertedBatches.single().size)
        assertEquals("xid1", placeOfInterestDao.insertedBatches.single().single().xid)
    }

    @Test
    fun `tile download failure short circuits without persisting anything`() = runTest {
        val offlineAreaDao = FakeOfflineAreaDao()
        val placeOfInterestDao = RecordingPlaceOfInterestDao()
        val repository = OfflineAreaRepositoryImpl(
            offlineAreaDao = offlineAreaDao,
            placeOfInterestDao = placeOfInterestDao,
            tileCacheDataSource = FakeTileCacheDataSource(
                downloadResult = listOf(DownloadProgress.Failed("disk full"))
            ),
            api = StubOpenTripMapApi()
        )

        val progressEvents = repository.saveOfflineArea("My area", testBoundingBox, 14, 16).toList()

        assertTrue(progressEvents.last() is DownloadProgress.Failed)
        assertEquals(0, offlineAreaDao.count())
        assertTrue(placeOfInterestDao.insertedBatches.isEmpty())
    }

    @Test
    fun `deleting the last remaining area purges the shared tile cache`() = runTest {
        val offlineAreaDao = FakeOfflineAreaDao()
        val areaId = offlineAreaDao.insert(
            OfflineAreaEntity(
                name = "Only area", north = 10.1, south = 10.0, east = 20.1, west = 20.0,
                minZoom = 14, maxZoom = 16, createdAtMillis = 0L, tileCount = 4, estimatedSizeBytes = 60_000
            )
        )
        val tileCacheDataSource = FakeTileCacheDataSource()
        val repository = OfflineAreaRepositoryImpl(
            offlineAreaDao = offlineAreaDao,
            placeOfInterestDao = RecordingPlaceOfInterestDao(),
            tileCacheDataSource = tileCacheDataSource,
            api = StubOpenTripMapApi()
        )

        val result = repository.deleteOfflineArea(areaId)

        assertTrue(result.isSuccess)
        assertTrue(tileCacheDataSource.purgeCalled)
    }

    @Test
    fun `deleting one area while another remains does not purge the tile cache`() = runTest {
        val offlineAreaDao = FakeOfflineAreaDao()
        val firstId = offlineAreaDao.insert(
            OfflineAreaEntity(
                name = "First", north = 10.1, south = 10.0, east = 20.1, west = 20.0,
                minZoom = 14, maxZoom = 16, createdAtMillis = 0L, tileCount = 4, estimatedSizeBytes = 60_000
            )
        )
        offlineAreaDao.insert(
            OfflineAreaEntity(
                name = "Second", north = 30.1, south = 30.0, east = 40.1, west = 40.0,
                minZoom = 14, maxZoom = 16, createdAtMillis = 0L, tileCount = 4, estimatedSizeBytes = 60_000
            )
        )
        val tileCacheDataSource = FakeTileCacheDataSource()
        val repository = OfflineAreaRepositoryImpl(
            offlineAreaDao = offlineAreaDao,
            placeOfInterestDao = RecordingPlaceOfInterestDao(),
            tileCacheDataSource = tileCacheDataSource,
            api = StubOpenTripMapApi()
        )

        val result = repository.deleteOfflineArea(firstId)

        assertTrue(result.isSuccess)
        assertFalse(tileCacheDataSource.purgeCalled)
        assertEquals(1, offlineAreaDao.count())
    }
}
