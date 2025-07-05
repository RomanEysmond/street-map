package com.example.example

import android.graphics.Canvas
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ZoomAwareMarker(
    mapView: MapView,
    private val minZoom: Double = 10.0,
    private val maxZoom: Double = 20.0
) : Marker(mapView) {

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val currentZoom = mapView.zoomLevelDouble
        if (currentZoom in minZoom..maxZoom) {
            super.draw(canvas, mapView, shadow)
        }
    }
}