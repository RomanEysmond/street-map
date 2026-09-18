package com.example.example.domain.repository

import kotlinx.coroutines.flow.Flow

interface ApiKeyRepository {
    fun observeHasApiKey(): Flow<Boolean>
    suspend fun getApiKey(): String?
    suspend fun saveApiKey(key: String)
}
