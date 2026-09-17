package com.example.example.data.repository.di

import com.example.example.data.connectivity.AndroidConnectivityChecker
import com.example.example.data.connectivity.ConnectivityChecker
import com.example.example.data.location.LocationRepositoryImpl
import com.example.example.data.offline.tiles.OsmdroidTileCacheDataSource
import com.example.example.data.offline.tiles.TileCacheDataSource
import com.example.example.data.repository.OfflineAreaRepositoryImpl
import com.example.example.data.repository.PlacesRepositoryImpl
import com.example.example.domain.repository.LocationRepository
import com.example.example.domain.repository.OfflineAreaRepository
import com.example.example.domain.repository.PlacesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlacesRepository(impl: PlacesRepositoryImpl): PlacesRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindOfflineAreaRepository(impl: OfflineAreaRepositoryImpl): OfflineAreaRepository

    @Binds
    @Singleton
    abstract fun bindTileCacheDataSource(impl: OsmdroidTileCacheDataSource): TileCacheDataSource

    @Binds
    @Singleton
    abstract fun bindConnectivityChecker(impl: AndroidConnectivityChecker): ConnectivityChecker
}
