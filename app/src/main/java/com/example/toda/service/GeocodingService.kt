package com.example.toda.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection
import kotlinx.coroutines.delay

class GeocodingService {
    private val TAG = "GeocodingService"
    private var lastApiCall = 0L
    private val API_DELAY = 1000L

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting dynamic reverse geocoding for: $latitude, $longitude")

                // Rate limiting for OSM API calls
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCall = currentTime - lastApiCall
                if (timeSinceLastCall < API_DELAY) {
                    delay(API_DELAY - timeSinceLastCall)
                }
                lastApiCall = System.currentTimeMillis()

                // Primary and only method: OSM reverse geocoding
                val osmAddress = fetchFromOSM(latitude, longitude)
                if (osmAddress != null) {
                    Log.d(TAG, "Successfully fetched from OSM: $osmAddress")
                    return@withContext osmAddress
                }

                // Simple fallback for when OSM doesn't return data
                Log.d(TAG, "OSM returned no data, using basic location fallback")
                return@withContext "Unknown Location"

            } catch (e: Exception) {
                Log.e(TAG, "Geocoding error: ${e.message}")
                return@withContext "Unknown Location"
            }
        }
    }

    private suspend fun fetchFromOSM(latitude: Double, longitude: Double): String? {
        return try {
            // Enhanced OSM query with multiple zoom levels for better accuracy
            val urls = listOf(
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1",
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=17&addressdetails=1",
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=16&addressdetails=1"
            )

            for (url in urls) {
                Log.d(TAG, "Trying OSM URL: $url")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "TODA-System/1.0 (Android App)")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Accept-Language", "en")
                    connectTimeout = 10000
                    readTimeout = 15000
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "OSM Response: $response")

                    val json = JSONObject(response)
                    val address = json.optJSONObject("address")
                    val displayName = json.optString("display_name", "")

                    if (address != null || displayName.isNotEmpty()) {
                        val formattedAddress = parseOSMResponse(address, displayName, latitude, longitude)
                        if (formattedAddress != null) {
                            return formattedAddress
                        }
                    }
                }

                // Small delay between attempts
                delay(500)
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "OSM fetch error: ${e.message}")
            null
        }
    }

    private fun parseOSMResponse(address: JSONObject?, displayName: String, latitude: Double, longitude: Double): String? {
        Log.d(TAG, "Parsing OSM response - address: $address, displayName: $displayName")

        // Priority order for address components - focusing on specific location names
        val addressComponents = mutableListOf<String>()

        if (address != null) {
            // Try to get specific address components in order of preference
            val road = address.optString("road", "")
            val pedestrian = address.optString("pedestrian", "")
            val footway = address.optString("footway", "")
            val path = address.optString("path", "")
            val houseNumber = address.optString("house_number", "")

            // Building/place specific
            val building = address.optString("building", "")
            val amenity = address.optString("amenity", "")
            val shop = address.optString("shop", "")
            val office = address.optString("office", "")

            // Only add the most specific location identifier
            when {
                road.isNotEmpty() -> {
                    addressComponents.add(if (houseNumber.isNotEmpty()) "$houseNumber $road" else road)
                }
                pedestrian.isNotEmpty() -> addressComponents.add(pedestrian)
                footway.isNotEmpty() -> addressComponents.add(footway)
                path.isNotEmpty() -> addressComponents.add(path)
                building.isNotEmpty() -> addressComponents.add(building)
                amenity.isNotEmpty() -> addressComponents.add(amenity)
                shop.isNotEmpty() -> addressComponents.add(shop)
                office.isNotEmpty() -> addressComponents.add(office)
            }
        }

        // If no specific components found, try to parse display_name for just the street name
        if (addressComponents.isEmpty() && displayName.isNotEmpty()) {
            val parts = displayName.split(",").map { it.trim() }
            // Take only the first part (most specific location)
            if (parts.isNotEmpty()) {
                addressComponents.add(parts[0])
            }
        }

        // Return address in the requested format: Street, Barangay 177, Caloocan City
        return when {
            addressComponents.isNotEmpty() -> "${addressComponents[0]}, Barangay 177, Caloocan City"
            else -> null
        }
    }

    /**
     * Validates if the given coordinates are within Barangay 177 boundaries
     */
    fun isWithinBarangay177(latitude: Double, longitude: Double): Boolean {
        // Central point: 14.7480, 121.0500 (near Cielito Homes area)
        val centerLat = 14.7480
        val centerLng = 121.0500
        val radiusKm = 0.6 // 0.6km radius

        // Calculate distance from center using Haversine formula
        val distance = calculateDistance(centerLat, centerLng, latitude, longitude)
        val isValid = distance <= radiusKm

        // Add debugging
        Log.d(TAG, "=== BARANGAY 177 RADIUS VALIDATION ===")
        Log.d(TAG, "Checking coordinates: $latitude, $longitude")
        Log.d(TAG, "Center point: $centerLat, $centerLng")
        Log.d(TAG, "Radius: ${radiusKm}km")
        Log.d(TAG, "Distance from center: ${String.format("%.3f", distance)}km")
        Log.d(TAG, "Within radius: $isValid")
        Log.d(TAG, "====================================")

        return isValid
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLngRad = Math.toRadians(lng2 - lng1)

        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLngRad / 2) * kotlin.math.sin(deltaLngRad / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Gets a user-friendly error message for locations outside Barangay 177
     */
    fun getLocationValidationMessage(latitude: Double, longitude: Double): String? {
        return if (!isWithinBarangay177(latitude, longitude)) {
            "Selected location is outside Barangay 177. Please select a location within the service area."
        } else null
    }

    /**
     * Enhanced forward geocoding with multiple search strategies
     * Useful for converting user-typed addresses to coordinates
     */
    suspend fun forwardGeocode(address: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Forward geocoding for: $address")

                // Rate limiting
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCall = currentTime - lastApiCall
                if (timeSinceLastCall < API_DELAY) {
                    delay(API_DELAY - timeSinceLastCall)
                }
                lastApiCall = System.currentTimeMillis()

                // Try multiple search strategies
                val searchQueries = listOf(
                    "$address, Barangay 177, Caloocan City, Philippines",
                    "$address, Caloocan City, Philippines",
                    "$address, Metro Manila, Philippines",
                    address.trim()
                )

                for (query in searchQueries) {
                    val encodedAddress = URLEncoder.encode(query, "UTF-8")
                    val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress&limit=3&bounded=1&viewbox=121.0350,14.7350,121.0620,14.7580"

                    Log.d(TAG, "Trying forward geocode URL: $url")

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "TODA-System/1.0 (Android App)")
                        setRequestProperty("Accept", "application/json")
                        connectTimeout = 10000
                        readTimeout = 15000
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = org.json.JSONArray(response)

                        if (jsonArray.length() > 0) {
                            // Get the best result (first one is usually most relevant)
                            val firstResult = jsonArray.getJSONObject(0)
                            val lat = firstResult.getDouble("lat")
                            val lon = firstResult.getDouble("lon")

                            Log.d(TAG, "Forward geocoding successful: $lat, $lon for query: $query")
                            return@withContext Pair(lat, lon)
                        }
                    }

                    // Small delay between different queries
                    delay(500)
                }

                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "Forward geocoding error: ${e.message}")
                return@withContext null
            }
        }
    }

    /**
     * Batch geocoding for multiple coordinates
     * Useful for processing multiple pickup/destination pairs
     */
    suspend fun batchReverseGeocode(coordinates: List<Pair<Double, Double>>): List<String?> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<String?>()

            for ((lat, lng) in coordinates) {
                try {
                    val address = reverseGeocode(lat, lng)
                    results.add(address)

                    // Respect rate limiting between batch requests
                    delay(API_DELAY)
                } catch (e: Exception) {
                    Log.e(TAG, "Batch geocoding error for $lat, $lng: ${e.message}")
                    results.add(null)
                }
            }

            results
        }
    }

    /**
     * Validates a user-typed address by geocoding it and checking if it's within Barangay 177
     * Returns null if valid, or error message if invalid
     */
    suspend fun validateTypedAddress(address: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // First try to geocode the address to get coordinates
                val coordinates = forwardGeocode(address)

                if (coordinates == null) {
                    return@withContext "Unable to find the specified address. Please check the spelling or try a different address within Barangay 177."
                }

                // Check if the coordinates are within Barangay 177
                val isValid = isWithinBarangay177(coordinates.first, coordinates.second)

                if (!isValid) {
                    return@withContext "The address '$address' is outside Barangay 177. Please enter an address within the service area."
                }

                // Address is valid
                return@withContext null

            } catch (e: Exception) {
                Log.e(TAG, "Address validation error: ${e.message}")
                return@withContext "Unable to validate address. Please try again or select a location on the map."
            }
        }
    }

    /**
     * Geocodes a user-typed address and returns formatted address with validation
     * Returns null if address is outside Barangay 177 or cannot be found
     */
    suspend fun geocodeAndValidateAddress(address: String): Pair<String?, Pair<Double, Double>?>? {
        return withContext(Dispatchers.IO) {
            try {
                // Get coordinates for the address
                val coordinates = forwardGeocode(address)

                if (coordinates == null) {
                    return@withContext null
                }

                // Validate coordinates are within Barangay 177
                val isValid = isWithinBarangay177(coordinates.first, coordinates.second)

                if (!isValid) {
                    return@withContext null
                }

                // Get formatted address for the coordinates
                val formattedAddress = reverseGeocode(coordinates.first, coordinates.second)

                return@withContext Pair(formattedAddress, coordinates)

            } catch (e: Exception) {
                Log.e(TAG, "Geocode and validate error: ${e.message}")
                return@withContext null
            }
        }
    }
}
