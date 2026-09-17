package com.example.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlacesResponseDto(
    val features: List<FeatureDto>? = null,
    val type: String
)
