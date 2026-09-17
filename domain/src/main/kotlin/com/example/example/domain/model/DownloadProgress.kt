package com.example.example.domain.model

sealed interface DownloadProgress {
    object Started : DownloadProgress
    data class TileProgress(val downloaded: Int, val total: Int) : DownloadProgress
    object PersistingPlaces : DownloadProgress
    data class Completed(val area: OfflineArea) : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}
