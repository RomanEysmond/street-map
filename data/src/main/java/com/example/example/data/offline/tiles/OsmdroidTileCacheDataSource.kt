package com.example.example.data.offline.tiles

import android.content.Context
import com.example.example.domain.model.DownloadProgress
import com.example.example.domain.model.GeoBoundingBox
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import javax.inject.Inject

/**
 * Wraps osmdroid's [CacheManager]. Built from [TileSourceFactory.MAPNIK] + a standalone
 * [SqlTileWriter] rather than a live MapView, so this data source has no UI/Activity dependency
 * and can be driven purely from the repository layer.
 */
class OsmdroidTileCacheDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : TileCacheDataSource {

    private fun newCacheManager(minZoom: Int, maxZoom: Int): CacheManager =
        CacheManager(TileSourceFactory.MAPNIK, SqlTileWriter(), minZoom, maxZoom)

    override fun estimateTileCount(boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Int =
        newCacheManager(minZoom, maxZoom).possibleTilesInArea(boundingBox.toOsmdroid(), minZoom, maxZoom)

    override fun downloadTiles(boundingBox: GeoBoundingBox, minZoom: Int, maxZoom: Int): Flow<DownloadProgress> =
        callbackFlow {
            val cacheManager = newCacheManager(minZoom, maxZoom)
            var totalTiles = 0

            val callback = object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    trySend(DownloadProgress.TileProgress(totalTiles, totalTiles))
                    close()
                }

                override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                    trySend(DownloadProgress.TileProgress(progress, totalTiles))
                }

                override fun downloadStarted() {
                    trySend(DownloadProgress.Started)
                }

                override fun setPossibleTilesInArea(total: Int) {
                    totalTiles = total
                }

                override fun onTaskFailed(errors: Int) {
                    // osmdroid fires this when the download finished but at least one tile
                    // failed (e.g. a flaky mobile connection) — the rest of the tiles are
                    // already cached, so treat this as a completed (partial) download rather
                    // than aborting the whole "save area" flow.
                    trySend(DownloadProgress.TileProgress(totalTiles, totalTiles))
                    close()
                }
            }

            cacheManager.downloadAreaAsyncNoUI(context, boundingBox.toOsmdroid(), minZoom, maxZoom, callback)

            awaitClose { cacheManager.cancelAllJobs() }
        }

    override fun purgeCache(): Boolean = SqlTileWriter().purgeCache()

    private fun GeoBoundingBox.toOsmdroid(): BoundingBox = BoundingBox(north, east, south, west)
}
