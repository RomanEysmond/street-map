package com.example.example.domain.usecase

import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.OfflineArea
import com.example.example.domain.repository.OfflineAreaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeOfflineAreaRepository : OfflineAreaRepository {
    var lastMinZoom: Int? = null
    var lastMaxZoom: Int? = null

    override fun observeOfflineAreas(): Flow<List<OfflineArea>> = flowOf(emptyList())

    override fun saveOfflineArea(name: String, boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Flow<DownloadProgress> {
        lastMinZoom = minZoom
        lastMaxZoom = maxZoom
        return flowOf(DownloadProgress.Started)
    }

    override suspend fun deleteOfflineArea(id: Long): Result<Unit> = Result.success(Unit)
}

class SaveOfflineAreaUseCaseTest {

    private val validBoundingBox = GeoBoundingBox(north = 10.1, south = 10.0, east = 20.1, west = 20.0)

    @Test
    fun `blank name fails without calling repository`() = runTest {
        val repository = FakeOfflineAreaRepository()
        val useCase = SaveOfflineAreaUseCase(repository)

        val result = useCase("   ", validBoundingBox, 14, 16).toList()

        assertTrue(result.single() is DownloadProgress.Failed)
        assertEquals(null, repository.lastMinZoom)
    }

    @Test
    fun `zoom levels are clamped to supported range`() = runTest {
        val repository = FakeOfflineAreaRepository()
        val useCase = SaveOfflineAreaUseCase(repository)

        useCase("My area", validBoundingBox, minZoom = 5, maxZoom = 40).toList()

        assertEquals(12, repository.lastMinZoom)
        assertEquals(19, repository.lastMaxZoom)
    }

    @Test
    fun `oversized area is rejected`() = runTest {
        val repository = FakeOfflineAreaRepository()
        val useCase = SaveOfflineAreaUseCase(repository)
        val hugeBoundingBox = GeoBoundingBox(north = 50.0, south = -50.0, east = 100.0, west = -100.0)

        val result = useCase("My area", hugeBoundingBox, 14, 16).toList()

        assertTrue(result.single() is DownloadProgress.Failed)
        assertEquals(null, repository.lastMinZoom)
    }
}
