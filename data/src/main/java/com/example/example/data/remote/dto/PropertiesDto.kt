package com.example.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PropertiesDto(
    val dist: Double,
    val kinds: String? = null,
    val name: String? = null,
    val osm: String? = null,
    val rate: Int,
    val wikidata: String? = null,
    val xid: String? = null
)
