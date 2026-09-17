package com.example.example.data.offline.tiles

import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import kotlinx.coroutines.flow.Flow

interface TileCacheDataSource {
    fun estimateTileCount(boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Int
    fun downloadTiles(boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Flow<DownloadProgress>

    /** Wipes the entire shared osmdroid tile cache. Only safe to call when no saved areas remain. */
    fun purgeCache(): Boolean
}
