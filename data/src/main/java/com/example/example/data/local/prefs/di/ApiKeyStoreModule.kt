package com.example.example.data.local.prefs.di

import com.example.example.data.local.prefs.ApiKeyStore
import com.example.example.data.local.prefs.EncryptedApiKeyStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiKeyStoreModule {

    @Binds
    @Singleton
    abstract fun bindApiKeyStore(impl: EncryptedApiKeyStore): ApiKeyStore
}
