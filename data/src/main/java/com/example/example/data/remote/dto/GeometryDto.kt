package com.example.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeometryDto(
    val coordinates: List<Double>,
    val type: String
)
