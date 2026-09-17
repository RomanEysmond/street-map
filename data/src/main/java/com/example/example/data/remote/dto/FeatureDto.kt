package com.example.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeatureDto(
    val geometry: GeometryDto,
    val id: String,
    val properties: PropertiesDto,
    val type: String
)
