package com.example.example.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "places_of_interest",
    indices = [Index(value = ["offlineAreaId"])]
)
data class PlaceOfInterestEntity(
    @PrimaryKey
    val xid: String,
    val offlineAreaId: Long,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val kinds: String?,
    val distanceMeters: Double?,
    val rating: Int?,
    val wikidataId: String?,
    val osmId: String?
)
