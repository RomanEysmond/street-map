package com.example.example

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://api.opentripmap.com/0.1/ru/places/"
//https://api.opentripmap.com/0.1/ru/places/radius?radius=400&lon=30.31743&lat=59.94980&apikey=5ae2e3f221c38a28845f05b647b74cb2289735e2188f24cd526f7128
object RetrofitServices {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val searchPlacesInfo: SearchPlaceApi = retrofit.create(SearchPlaceApi::class.java)
}

interface SearchPlaceApi {
    @GET("radius?radius=1000")
    suspend fun getPlaceInfo(
        @Query("lon") lon: Double,
        @Query("lat") lat: Double,
        @Query("apikey") apiKey: String = "5ae2e3f221c38a28845f05b647b74cb2289735e2188f24cd526f7128"
    ): Places
}