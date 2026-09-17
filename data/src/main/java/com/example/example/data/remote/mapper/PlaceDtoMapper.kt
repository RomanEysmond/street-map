package com.example.example.data.remote.mapper

import com.example.example.data.remote.dto.FeatureDto
import com.example.example.domain.model.Coordinates
import com.example.example.domain.model.PlaceOfInterest

/**
 * GeoJSON orders geometry coordinates as [longitude, latitude], not [latitude, longitude].
 */
fun FeatureDto.toDomain(): PlaceOfInterest {
    val coordinates = Coordinates(
        latitude = geometry.coordinates.last(),
        longitude = geometry.coordinates.first()
    )
    return PlaceOfInterest(
        id = properties.xid ?: id,
        name = properties.name,
        coordinates = coordinates,
        kinds = properties.kinds,
        distanceMeters = properties.dist,
        rating = properties.rate,
        wikidataId = properties.wikidata,
        osmId = properties.osm
    )
}
