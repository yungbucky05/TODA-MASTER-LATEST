package com.example.toda.service

import com.example.toda.data.RouteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface OpenRouteServiceApi {
    @GET("v2/directions/driving-car")
    suspend fun getRoute(
        @Header("Authorization") apiKey: String,
        @Query("start") start: String, // Format: "longitude,latitude"
        @Query("end") end: String     // Format: "longitude,latitude"
    ): Response<RouteResponse>
}
