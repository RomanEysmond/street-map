package com.example.example.data.repository

import com.example.example.data.local.prefs.ApiKeyStore
import com.example.example.domain.repository.ApiKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ApiKeyRepositoryImpl @Inject constructor(
    private val apiKeyStore: ApiKeyStore
) : ApiKeyRepository {

    override fun observeHasApiKey(): Flow<Boolean> = apiKeyStore.observeHasKey()

    override suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        apiKeyStore.get()
    }

    override suspend fun saveApiKey(key: String) = withContext(Dispatchers.IO) {
        apiKeyStore.save(key)
    }
}
