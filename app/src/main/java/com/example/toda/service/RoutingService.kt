package com.example.toda.service

import android.util.Log
import com.example.toda.BuildConfig
import com.example.toda.data.RouteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingService @Inject constructor() {

    private val api: OpenRouteServiceApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG_MODE) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(OpenRouteServiceApi::class.java)
    }

    suspend fun getRoute(startPoint: GeoPoint, endPoint: GeoPoint): Result<List<GeoPoint>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("RoutingService", "Calculating route from ${startPoint.latitude},${startPoint.longitude} to ${endPoint.latitude},${endPoint.longitude}")

                val start = "${startPoint.longitude},${startPoint.latitude}"
                val end = "${endPoint.longitude},${endPoint.latitude}"
                val apiKey = BuildConfig.OPENROUTE_API_KEY

                val response = api.getRoute(apiKey, start, end)

                if (response.isSuccessful) {
                    val routeResponse = response.body()
                    if (routeResponse != null && routeResponse.features.isNotEmpty()) {
                        val coordinates = routeResponse.features[0].geometry.coordinates
                        val geoPoints = coordinates.map { coord ->
                            // OpenRouteService returns [longitude, latitude]
                            GeoPoint(coord[1], coord[0])
                        }
                        Log.d("RoutingService", "Route calculated successfully with ${geoPoints.size} points")
                        Result.success(geoPoints)
                    } else {
                        Log.e("RoutingService", "Empty route response")
                        Result.failure(Exception("Empty route response"))
                    }
                } else {
                    Log.e("RoutingService", "Route API error: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Route calculation failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("RoutingService", "Error calculating route", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getRouteWithFallback(startPoint: GeoPoint, endPoint: GeoPoint): List<GeoPoint> {
        return getRoute(startPoint, endPoint).getOrElse {
            Log.w("RoutingService", "Route calculation failed, using straight line fallback")
            // Fallback to straight line if routing fails
            listOf(startPoint, endPoint)
        }
    }
}
