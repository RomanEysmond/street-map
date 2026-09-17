package com.example.example.domain.repository

import com.example.example.domain.model.Coordinates
import com.example.example.domain.model.PlaceOfInterest

interface PlacesRepository {
    suspend fun getNearbyPlaces(
        coordinates: Coordinates,
        radiusMeters: Int = 1000
    ): Result<List<PlaceOfInterest>>
}
