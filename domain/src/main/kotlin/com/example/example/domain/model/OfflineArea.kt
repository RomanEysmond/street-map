package com.example.example.domain.model

data class OfflineArea(
    val id: Long,
    val name: String,
    val boundingBox: GeoBoundingBox,
    val minZoom: Int,
    val maxZoom: Int,
    val createdAtMillis: Long,
    val tileCount: Int,
    val estimatedSizeBytes: Long
)
