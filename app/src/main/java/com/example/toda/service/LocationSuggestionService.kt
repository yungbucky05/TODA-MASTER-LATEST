package com.example.toda.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*

data class LocationSuggestion(
    val id: String,
    val name: String,
    val address: String,
    val geoPoint: GeoPoint,
    val type: LocationType = LocationType.GENERAL,
    val osmId: String? = null,
    val category: String? = null
)

enum class LocationType {
    GENERAL,
    LANDMARK,
    BUSINESS,
    RESIDENTIAL,
    SCHOOL,
    HEALTH,
    GOVERNMENT,
    RELIGIOUS,
    TRANSPORT,
    RESTAURANT,
    SHOPPING
}

class LocationSuggestionService(private val context: Context) {

    // Barangay 177 center coordinates
    private val barangay177Center = GeoPoint(14.7487, 121.04951)
    private val serviceRadius = 1.5 // 1.5km radius

    // Cache for frequently searched locations
    private val locationCache = mutableMapOf<String, List<LocationSuggestion>>()
    private val geocodeCache = mutableMapOf<String, GeoPoint?>()

    // Predefined important locations in Barangay 177 area
    private val predefinedLocations = listOf(
        LocationSuggestion("brgy177_hall", "Barangay 177 Hall", "Barangay 177, Caloocan City", GeoPoint(14.7487, 121.04951), LocationType.GOVERNMENT),
        LocationSuggestion("brgy177_health", "Barangay 177 Health Center", "Health Center Road, Barangay 177, Caloocan", GeoPoint(14.7482, 121.0501), LocationType.HEALTH),
        LocationSuggestion("brgy177_school", "Barangay 177 Elementary School", "School Area, Barangay 177, Caloocan", GeoPoint(14.7495, 121.0512), LocationType.SCHOOL),
        LocationSuggestion("bagong_silang_market", "Bagong Silang Public Market", "Phase 5, Bagong Silang, Caloocan", GeoPoint(14.7502, 121.0525), LocationType.BUSINESS),
        LocationSuggestion("bagong_silang_mrt", "Bagong Silang MRT Station", "North Avenue, Bagong Silang, Caloocan", GeoPoint(14.7534, 121.0512), LocationType.TRANSPORT),
        LocationSuggestion("ucc_main", "University of Caloocan City", "Biglang Awa Street, Caloocan", GeoPoint(14.7523, 121.0445), LocationType.SCHOOL),
        LocationSuggestion("camarin_market", "Camarin Public Market", "Camarin Road, Caloocan", GeoPoint(14.7445, 121.0441), LocationType.BUSINESS)
    )

    suspend fun searchLocations(query: String): List<LocationSuggestion> {
        if (query.isBlank() || query.length < 2) return predefinedLocations.take(5)

        // Check cache first
        val cacheKey = query.lowercase().trim()
        locationCache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<LocationSuggestion>()

                // Add matching predefined locations first
                val predefinedMatches = predefinedLocations.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.address.contains(query, ignoreCase = true)
                }
                results.addAll(predefinedMatches)

                // Search OpenStreetMap/Nominatim for additional locations
                val osmResults = searchNominatim(query)
                results.addAll(osmResults)

                // Remove duplicates and limit results
                val uniqueResults = results.distinctBy { it.name }.take(8)

                // Cache results
                locationCache[cacheKey] = uniqueResults

                uniqueResults
            } catch (e: Exception) {
                // Fallback to predefined locations if network fails
                predefinedLocations.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.address.contains(query, ignoreCase = true)
                }.take(5)
            }
        }
    }

    private suspend fun searchNominatim(query: String): List<LocationSuggestion> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val bounds = getBoundingBox()
                val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery" +
                        "&format=json" +
                        "&addressdetails=1" +
                        "&limit=10" +
                        "&bounded=1" +
                        "&viewbox=${bounds.west},${bounds.south},${bounds.east},${bounds.north}" +
                        "&countrycodes=ph"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TODA-App/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                        it.readText()
                    }
                    parseNominatimResponse(response)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseNominatimResponse(jsonResponse: String): List<LocationSuggestion> {
        val results = mutableListOf<LocationSuggestion>()

        try {
            val jsonArray = org.json.JSONArray(jsonResponse)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val lat = item.getDouble("lat")
                val lon = item.getDouble("lon")
                val geoPoint = GeoPoint(lat, lon)

                // Only include locations within service area
                if (!isWithinServiceArea(geoPoint)) continue

                val displayName = item.getString("display_name")
                val osmId = item.optString("osm_id", "")
                val osmType = item.optString("osm_type", "")
                val category = item.optString("category", "")
                val type = item.optString("type", "")

                // Extract meaningful name and address
                val nameParts = displayName.split(",")
                val name = if (nameParts.isNotEmpty()) {
                    nameParts[0].trim()
                } else {
                    "Unknown Location"
                }

                val address = if (nameParts.size > 1) {
                    nameParts.drop(1).joinToString(", ").trim()
                } else {
                    displayName
                }

                val locationType = mapOsmTypeToLocationType(category, type)

                results.add(
                    LocationSuggestion(
                        id = "${osmType}_${osmId}",
                        name = name,
                        address = address,
                        geoPoint = geoPoint,
                        type = locationType,
                        osmId = osmId,
                        category = category
                    )
                )
            }
        } catch (e: Exception) {
            // Return empty list if parsing fails
        }

        return results
    }

    private fun mapOsmTypeToLocationType(category: String, type: String): LocationType {
        return when {
            category == "amenity" && type in listOf("school", "university", "college") -> LocationType.SCHOOL
            category == "amenity" && type in listOf("hospital", "clinic", "pharmacy", "doctors") -> LocationType.HEALTH
            category == "amenity" && type in listOf("townhall", "government", "police", "fire_station") -> LocationType.GOVERNMENT
            category == "amenity" && type in listOf("place_of_worship", "church") -> LocationType.RELIGIOUS
            category == "amenity" && type in listOf("restaurant", "fast_food", "cafe", "food_court") -> LocationType.RESTAURANT
            category == "shop" || (category == "amenity" && type in listOf("marketplace", "mall")) -> LocationType.SHOPPING
            category == "public_transport" || type in listOf("bus_station", "station") -> LocationType.TRANSPORT
            category == "landuse" && type == "residential" -> LocationType.RESIDENTIAL
            type in listOf("monument", "memorial", "attraction") -> LocationType.LANDMARK
            else -> LocationType.GENERAL
        }
    }

    suspend fun geocodeAddress(address: String): GeoPoint? {
        if (address.isBlank()) return null

        // Check cache first
        val cacheKey = address.lowercase().trim()
        geocodeCache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val encodedAddress = URLEncoder.encode(address, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$encodedAddress" +
                        "&format=json" +
                        "&limit=1" +
                        "&countrycodes=ph"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TODA-App/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                        it.readText()
                    }

                    val jsonArray = org.json.JSONArray(response)
                    if (jsonArray.length() > 0) {
                        val item = jsonArray.getJSONObject(0)
                        val lat = item.getDouble("lat")
                        val lon = item.getDouble("lon")
                        val geoPoint = GeoPoint(lat, lon)

                        // Cache result
                        geocodeCache[cacheKey] = geoPoint
                        geoPoint
                    } else {
                        geocodeCache[cacheKey] = null
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun reverseGeocode(geoPoint: GeoPoint): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?lat=${geoPoint.latitude}" +
                        "&lon=${geoPoint.longitude}" +
                        "&format=json" +
                        "&addressdetails=1"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TODA-App/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                        it.readText()
                    }

                    val jsonObject = JSONObject(response)
                    jsonObject.optString("display_name", null)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getNearbyLocations(geoPoint: GeoPoint, radiusKm: Double = 1.0): List<LocationSuggestion> {
        return predefinedLocations.filter { location ->
            calculateDistance(geoPoint, location.geoPoint) <= radiusKm
        }.sortedBy { calculateDistance(geoPoint, it.geoPoint) }
    }

    fun getLocationsByType(type: LocationType): List<LocationSuggestion> {
        return predefinedLocations.filter { it.type == type }
    }

    fun getAllPredefinedLocations(): List<LocationSuggestion> {
        return predefinedLocations
    }

    fun isWithinServiceArea(geoPoint: GeoPoint): Boolean {
        return calculateDistance(geoPoint, barangay177Center) <= serviceRadius
    }

    private fun getBoundingBox(): BoundingBox {
        val deltaLat = serviceRadius / 111.0 // Approximate degrees
        val deltaLon = serviceRadius / (111.0 * cos(Math.toRadians(barangay177Center.latitude)))

        return BoundingBox(
            north = barangay177Center.latitude + deltaLat,
            south = barangay177Center.latitude - deltaLat,
            east = barangay177Center.longitude + deltaLon,
            west = barangay177Center.longitude - deltaLon
        )
    }

    private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val earthRadius = 6371.0
        val lat1Rad = Math.toRadians(point1.latitude)
        val lon1Rad = Math.toRadians(point1.longitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val lon2Rad = Math.toRadians(point2.longitude)

        val deltaLat = lat2Rad - lat1Rad
        val deltaLon = lon2Rad - lon1Rad

        val a = sin(deltaLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(deltaLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun clearCache() {
        locationCache.clear()
        geocodeCache.clear()
    }
}

data class BoundingBox(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)