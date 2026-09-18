package com.example.example.data.local.prefs

import kotlinx.coroutines.flow.Flow

interface ApiKeyStore {
    fun get(): String?
    fun save(key: String)
    fun observeHasKey(): Flow<Boolean>
}
