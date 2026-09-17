package com.example.example.offline

import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.OfflineArea
import com.example.example.domain.repository.OfflineAreaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeOfflineAreaRepository : OfflineAreaRepository {
    val areasFlow = MutableStateFlow<List<OfflineArea>>(emptyList())
    val deletedIds = mutableListOf<Long>()

    override fun observeOfflineAreas(): Flow<List<OfflineArea>> = areasFlow

    override fun saveOfflineArea(name: String, boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Flow<DownloadProgress> =
        flowOf(DownloadProgress.Started)

    override suspend fun deleteOfflineArea(id: Long): Result<Unit> {
        deletedIds.add(id)
        areasFlow.value = areasFlow.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}

private fun sampleArea(id: Long) = OfflineArea(
    id = id,
    name = "Area $id",
    boundingBox = GeoBoundingBox(north = 1.0, south = 0.0, east = 1.0, west = 0.0),
    minZoom = 14,
    maxZoom = 16,
    createdAtMillis = 0L,
    tileCount = 4,
    estimatedSizeBytes = 60_000
)

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineAreasViewModelTest {

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
    fun `areas reflect repository state`() = runTest {
        val repository = FakeOfflineAreaRepository()
        repository.areasFlow.value = listOf(sampleArea(1), sampleArea(2))

        val viewModel = OfflineAreasViewModel(repository)
        // areas is a WhileSubscribed StateFlow: it only starts collecting the repository once
        // something subscribes, so an active collector is required to observe updates in a test.
        val collectorJob = launch { viewModel.areas.collect {} }
        advanceUntilIdle()

        assertEquals(2, viewModel.areas.value.size)

        collectorJob.cancel()
    }

    @Test
    fun `delete click removes the area via the repository`() = runTest {
        val repository = FakeOfflineAreaRepository()
        repository.areasFlow.value = listOf(sampleArea(1))
        val viewModel = OfflineAreasViewModel(repository)
        val collectorJob = launch { viewModel.areas.collect {} }
        advanceUntilIdle()
        assertEquals(1, viewModel.areas.value.size)

        viewModel.onDeleteClicked(1)
        advanceUntilIdle()

        assertTrue(repository.deletedIds.contains(1L))
        assertTrue(viewModel.areas.value.isEmpty())

        collectorJob.cancel()
    }
}
