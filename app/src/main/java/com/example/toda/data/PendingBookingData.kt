package com.example.toda.data

import android.content.Context
import org.osmdroid.util.GeoPoint

// Stores minimal information needed to restore a customer's last attempted booking
data class PendingBookingData(
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val pickupLocationName: String? = null,
    val destinationName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun pickupPoint(): GeoPoint = GeoPoint(pickupLat, pickupLng)
    fun dropoffPoint(): GeoPoint = GeoPoint(dropoffLat, dropoffLng)
}

object PendingBookingStore {
    private const val PREFS_NAME = "pending_booking_prefs"
    private const val KEY_HAS = "has_pending"
    private const val KEY_PICKUP_LAT = "pickup_lat"
    private const val KEY_PICKUP_LNG = "pickup_lng"
    private const val KEY_DROPOFF_LAT = "dropoff_lat"
    private const val KEY_DROPOFF_LNG = "dropoff_lng"
    private const val KEY_PICKUP_NAME = "pickup_name"
    private const val KEY_DEST_NAME = "dest_name"
    private const val KEY_TIMESTAMP = "timestamp"

    fun save(context: Context, data: PendingBookingData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_HAS, true)
            .putLong(KEY_TIMESTAMP, data.timestamp)
            .putString(KEY_PICKUP_NAME, data.pickupLocationName)
            .putString(KEY_DEST_NAME, data.destinationName)
            .putString(KEY_PICKUP_LAT, data.pickupLat.toString())
            .putString(KEY_PICKUP_LNG, data.pickupLng.toString())
            .putString(KEY_DROPOFF_LAT, data.dropoffLat.toString())
            .putString(KEY_DROPOFF_LNG, data.dropoffLng.toString())
            .apply()
    }

    fun load(context: Context): PendingBookingData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS, false)) return null

        val pickupLat = prefs.getString(KEY_PICKUP_LAT, null)?.toDoubleOrNull()
        val pickupLng = prefs.getString(KEY_PICKUP_LNG, null)?.toDoubleOrNull()
        val dropoffLat = prefs.getString(KEY_DROPOFF_LAT, null)?.toDoubleOrNull()
        val dropoffLng = prefs.getString(KEY_DROPOFF_LNG, null)?.toDoubleOrNull()
        val pickupName = prefs.getString(KEY_PICKUP_NAME, null)
        val destName = prefs.getString(KEY_DEST_NAME, null)
        val ts = prefs.getLong(KEY_TIMESTAMP, 0L)

        return if (pickupLat != null && pickupLng != null && dropoffLat != null && dropoffLng != null) {
            PendingBookingData(
                pickupLat = pickupLat,
                pickupLng = pickupLng,
                dropoffLat = dropoffLat,
                dropoffLng = dropoffLng,
                pickupLocationName = pickupName,
                destinationName = destName,
                timestamp = ts
            )
        } else {
            null
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
