package com.example.example.data.repository

import com.example.example.data.local.prefs.ApiKeyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeApiKeyStore : ApiKeyStore {
    private var storedKey: String? = null
    val hasKeyFlow = MutableStateFlow(false)

    override fun get(): String? = storedKey

    override fun save(key: String) {
        storedKey = key
        hasKeyFlow.value = key.isNotBlank()
    }

    override fun observeHasKey(): Flow<Boolean> = hasKeyFlow
}

class ApiKeyRepositoryImplTest {

    @Test
    fun `getApiKey returns null before any key is saved`() = runTest {
        val repository = ApiKeyRepositoryImpl(FakeApiKeyStore())

        assertNull(repository.getApiKey())
    }

    @Test
    fun `saveApiKey persists the key and getApiKey returns it`() = runTest {
        val repository = ApiKeyRepositoryImpl(FakeApiKeyStore())

        repository.saveApiKey("my-key")

        assertEquals("my-key", repository.getApiKey())
    }

    @Test
    fun `observeHasApiKey reflects the store after saving`() = runTest {
        val store = FakeApiKeyStore()
        val repository = ApiKeyRepositoryImpl(store)

        repository.saveApiKey("my-key")

        assertTrue(store.hasKeyFlow.value)
    }
}
