package com.example.example.domain.usecase

import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.repository.OfflineAreaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SaveOfflineAreaUseCase @Inject constructor(
    private val offlineAreaRepository: OfflineAreaRepository
) {
    operator fun invoke(
        name: String,
        boundingBox: GeoBoundingBox,
        minZoom: Int,
        maxZoom: Int
    ): Flow<DownloadProgress> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return flowOf(DownloadProgress.Failed("Area name must not be blank"))
        }

        val clampedMinZoom = minZoom.coerceIn(MIN_SUPPORTED_ZOOM, MAX_SUPPORTED_ZOOM)
        val clampedMaxZoom = maxZoom.coerceIn(clampedMinZoom, MAX_SUPPORTED_ZOOM)

        val latSpan = boundingBox.north - boundingBox.south
        val lonSpan = boundingBox.east - boundingBox.west
        if (latSpan <= 0 || lonSpan <= 0 || latSpan > MAX_AREA_SPAN_DEGREES || lonSpan > MAX_AREA_SPAN_DEGREES) {
            return flowOf(DownloadProgress.Failed("Selected area is invalid or too large to save offline"))
        }

        return offlineAreaRepository.saveOfflineArea(trimmedName, boundingBox, clampedMinZoom, clampedMaxZoom)
    }

    private companion object {
        const val MIN_SUPPORTED_ZOOM = 12
        const val MAX_SUPPORTED_ZOOM = 19
        const val MAX_AREA_SPAN_DEGREES = 0.5
    }
}
