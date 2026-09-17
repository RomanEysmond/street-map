package com.example.example.domain.repository

import com.example.example.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocationUpdates(): Flow<UserLocation>
}
