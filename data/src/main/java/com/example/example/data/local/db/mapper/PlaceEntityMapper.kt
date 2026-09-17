package com.example.example.data.local.db.mapper

import com.example.example.data.local.db.entity.PlaceOfInterestEntity
import com.example.example.domain.model.Coordinates
import com.example.example.domain.model.PlaceOfInterest

fun PlaceOfInterestEntity.toDomain(): PlaceOfInterest = PlaceOfInterest(
    id = xid,
    name = name,
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    kinds = kinds,
    distanceMeters = distanceMeters,
    rating = rating,
    wikidataId = wikidataId,
    osmId = osmId
)

fun PlaceOfInterest.toEntity(offlineAreaId: Long): PlaceOfInterestEntity = PlaceOfInterestEntity(
    xid = id,
    offlineAreaId = offlineAreaId,
    name = name,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    kinds = kinds,
    distanceMeters = distanceMeters,
    rating = rating,
    wikidataId = wikidataId,
    osmId = osmId
)
