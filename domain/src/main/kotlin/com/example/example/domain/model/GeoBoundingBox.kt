package com.example.example.domain.model

data class GeoBoundingBox(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
) {
    fun contains(coordinates: Coordinates): Boolean {
        return coordinates.latitude in south..north && coordinates.longitude in west..east
    }
}
