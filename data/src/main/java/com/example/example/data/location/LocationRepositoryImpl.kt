package com.example.example.data.location

import com.example.example.domain.model.UserLocation
import com.example.example.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val fusedLocationDataSource: FusedLocationDataSource
) : LocationRepository {
    override fun observeLocationUpdates(): Flow<UserLocation> = fusedLocationDataSource.observeLocationUpdates()
}
