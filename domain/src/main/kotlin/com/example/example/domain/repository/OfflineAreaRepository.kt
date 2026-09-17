package com.example.example.domain.repository

import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.OfflineArea
import kotlinx.coroutines.flow.Flow

interface OfflineAreaRepository {
    fun observeOfflineAreas(): Flow<List<OfflineArea>>

    fun saveOfflineArea(
        name: String,
        boundingBox: GeoBoundingBox,
        minZoom: Int,
        maxZoom: Int
    ): Flow<DownloadProgress>

    suspend fun deleteOfflineArea(id: Long): Result<Unit>
}
