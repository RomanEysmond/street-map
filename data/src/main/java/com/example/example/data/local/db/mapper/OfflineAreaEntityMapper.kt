package com.example.example.data.local.db.mapper

import com.example.example.data.local.db.entity.OfflineAreaEntity
import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.OfflineArea

fun OfflineAreaEntity.toDomain(): OfflineArea = OfflineArea(
    id = id,
    name = name,
    boundingBox = GeoBoundingBox(north = north, south = south, east = east, west = west),
    minZoom = minZoom,
    maxZoom = maxZoom,
    createdAtMillis = createdAtMillis,
    tileCount = tileCount,
    estimatedSizeBytes = estimatedSizeBytes
)

fun OfflineArea.toEntity(): OfflineAreaEntity = OfflineAreaEntity(
    id = id,
    name = name,
    north = boundingBox.north,
    south = boundingBox.south,
    east = boundingBox.east,
    west = boundingBox.west,
    minZoom = minZoom,
    maxZoom = maxZoom,
    createdAtMillis = createdAtMillis,
    tileCount = tileCount,
    estimatedSizeBytes = estimatedSizeBytes
)
