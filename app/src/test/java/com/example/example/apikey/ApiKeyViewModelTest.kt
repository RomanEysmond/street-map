package com.example.example.apikey

import com.example.example.domain.repository.ApiKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeApiKeyRepository(initialKey: String? = null) : ApiKeyRepository {
    private var storedKey: String? = initialKey
    val hasApiKeyFlow = MutableStateFlow(!initialKey.isNullOrBlank())
    val savedKeys = mutableListOf<String>()

    override fun observeHasApiKey(): Flow<Boolean> = hasApiKeyFlow

    override suspend fun getApiKey(): String? = storedKey

    override suspend fun saveApiKey(key: String) {
        storedKey = key
        savedKeys.add(key)
        hasApiKeyFlow.value = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ApiKeyViewModelTest {

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
    fun `starts unsaved when no key is stored yet`() = runTest {
        val viewModel = ApiKeyViewModel(FakeApiKeyRepository(initialKey = null))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved.not())
    }

    @Test
    fun `already saved key skips straight through on init`() = runTest {
        val viewModel = ApiKeyViewModel(FakeApiKeyRepository(initialKey = "existing-key"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `blank input is rejected with an error and not saved`() = runTest {
        val repository = FakeApiKeyRepository()
        val viewModel = ApiKeyViewModel(repository)
        advanceUntilIdle()

        viewModel.onSaveClicked("   ")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.saved.not())
        assertTrue(repository.savedKeys.isEmpty())
    }

    @Test
    fun `non-blank input is saved and marks the state as saved`() = runTest {
        val repository = FakeApiKeyRepository()
        val viewModel = ApiKeyViewModel(repository)
        advanceUntilIdle()

        viewModel.onSaveClicked("my-real-key")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.saved)
        assertEquals(listOf("my-real-key"), repository.savedKeys)
    }
}
