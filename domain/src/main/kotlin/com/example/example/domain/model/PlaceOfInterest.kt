package com.example.example.domain.model

data class PlaceOfInterest(
    val id: String,
    val name: String?,
    val coordinates: Coordinates,
    val kinds: String?,
    val distanceMeters: Double?,
    val rating: Int?,
    val wikidataId: String?,
    val osmId: String?
)
