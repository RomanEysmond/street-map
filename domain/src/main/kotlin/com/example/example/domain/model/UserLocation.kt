package com.example.example.domain.model

data class UserLocation(
    val coordinates: Coordinates,
    val accuracyMeters: Float?,
    val timestampMillis: Long
)
