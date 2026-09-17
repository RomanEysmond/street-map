package com.example.example.data.remote

import com.example.example.data.remote.dto.PlacesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenTripMapApi {
    @GET("radius")
    suspend fun getPlaceInfo(
        @Query("lon") lon: Double,
        @Query("lat") lat: Double,
        @Query("radius") radiusMeters: Int
    ): PlacesResponseDto
}
