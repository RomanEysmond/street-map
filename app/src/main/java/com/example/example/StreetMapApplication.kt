package com.example.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class StreetMapApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val osmConfig = Configuration.getInstance()
        osmConfig.load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )
        // OSM's tile usage policy requires a distinguishing User-Agent; osmdroid's
        // default is a generic string OSM now blocks. Must be set before any tile request.
        osmConfig.userAgentValue = BuildConfig.APPLICATION_ID
        // Explicit, conservative concurrency for offline-area bulk downloads too —
        // avoid relying on an implicit default that a future change could raise.
        osmConfig.tileDownloadThreads = 2
        osmConfig.tileDownloadMaxQueueSize = 4
    }
}
