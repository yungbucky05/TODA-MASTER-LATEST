package com.example.toda.service

import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.toda.data.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseRealtimeDatabaseService {

    private val database: DatabaseReference = Firebase.database.reference

    init {
        // Add debugging for Firebase database connection
        println("=== FIREBASE DATABASE SERVICE INIT ===")
        println("Database reference: ${database.ref}")
        println("Database URL: ${Firebase.database.app.options.databaseUrl}")
        println("==========================================")
    }

    // Database references for different collections
    private val usersRef = database.child("users")
    private val userProfilesRef = database.child("userProfiles")
    private val bookingsRef = database.child("bookings")
    private val tricyclesRef = database.child("tricycles")
    private val driverRegistrationsRef = database.child("driverRegistrations")
    private val todaOrganizationsRef = database.child("todaOrganizations")
    private val driverLocationsRef = database.child("driverLocations")
    private val chatMessagesRef = database.child("chatMessages")
    private val chatRoomsRef = database.child("chatRooms")
    private val notificationsRef = database.child("notifications")
    private val emergencyAlertsRef = database.child("emergencyAlerts")

    // Index references for efficient querying
    private val phoneNumberIndexRef = database.child("phoneNumberIndex")
    private val activeBookingsRef = database.child("activeBookings")
    private val availableDriversRef = database.child("availableDrivers")
    private val pendingApplicationsRef = database.child("pendingApplications")
    private val bookingIndexRef = database.child("bookingIndex") // Add booking index reference

    // Hardware Integration Methods (ESP32 Support)
    private val hardwareDriversRef = database.child("drivers")
    private val driverQueueRef = database.child("driverQueue")
    private val contributionsRef = database.child("contributions")
    private val activeTripsRef = database.child("activeTrips")
    private val systemStatusRef = database.child("systemStatus")
    private val ratingsRef = database.child("ratings") // Add ratings reference
    private val rfidChangeHistoryRef = database.child("rfidChangeHistory") // Add RFID change history reference

    // User Management
    suspend fun createUser(user: FirebaseUser): Boolean {
        return try {
            usersRef.child(user.id).setValue(user).await()
            // Add to phone number index for quick lookup
            phoneNumberIndexRef.child(user.phoneNumber).setValue(user.id).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Create user with additional data for detailed registration
    suspend fun createUserWithData(userId: String, userData: Map<String, Any>): Boolean {
        return try {
            // Save the user data to the users node
            usersRef.child(userId).setValue(userData).await()

            // Add to phone number index for quick lookup
            val phoneNumber = userData["phoneNumber"] as? String ?: ""
            if (phoneNumber.isNotBlank()) {
                phoneNumberIndexRef.child(phoneNumber).setValue(userId).await()
            }
            true
        } catch (e: Exception) {
            println("Error creating user with data: ${e.message}")
            false
        }
    }

    suspend fun getUserByPhoneNumber(phoneNumber: String): FirebaseUser? {
        return try {
            val userId = phoneNumberIndexRef.child(phoneNumber).get().await().getValue(String::class.java)
            userId?.let {
                usersRef.child(it).get().await().getValue(FirebaseUser::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByUserId(userId: String): FirebaseUser? {
        return try {
            usersRef.child(userId).get().await().getValue(FirebaseUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(userId: String, profile: FirebaseUserProfile): Boolean {
        return try {
            userProfilesRef.child(userId).setValue(profile).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserProfile(userId: String): Flow<FirebaseUserProfile?> = callbackFlow {
        val listener = userProfilesRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(FirebaseUserProfile::class.java)
                trySend(profile)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        })
        awaitClose { userProfilesRef.child(userId).removeEventListener(listener) }
    }

    // New: stream raw user data from 'users' node to access discount fields stored there
    fun getUserRaw(userId: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = usersRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value
                @Suppress("UNCHECKED_CAST")
                val map = value as? Map<String, Any>
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        })
        awaitClose { usersRef.child(userId).removeEventListener(listener) }
    }

    // Booking Management
    suspend fun createBooking(booking: FirebaseBooking): String? {
        return try {
            println("=== FIREBASE CREATE BOOKING DEBUG ===")
            println("Attempting to create booking: $booking")

            // First, test basic Firebase connectivity
            println("Testing Firebase connectivity...")
            val testRef = database.child("test").push()
            testRef.setValue(mapOf("timestamp" to System.currentTimeMillis())).await()
            println("Firebase connectivity test PASSED")

            val bookingRef = bookingsRef.push()
            val bookingId = bookingRef.key

            println("Generated booking ID: $bookingId")

            if (bookingId == null) {
                println("ERROR: Failed to generate booking ID")
                return null
            }

            val bookingWithId = booking.copy(id = bookingId)
            println("Booking with ID: $bookingWithId")

            println("Attempting to save to Firebase...")
            bookingRef.setValue(bookingWithId).await()
            println("SUCCESS: Booking saved to Firebase with ID: $bookingId")

            // Create bookingIndex entry for ESP32 efficient lookup
            // Similar to rfidUIDIndex, this contains minimal data for quick access
            println("Creating bookingIndex entry for ESP32...")
            val bookingIndexEntry = mapOf(
                "status" to booking.status,
                "timestamp" to booking.timestamp,
                "driverRFID" to (booking.driverRFID ?: "")
            )

            bookingIndexRef.child(bookingId).setValue(bookingIndexEntry).await()
            println("SUCCESS: bookingIndex entry created: $bookingId -> status=${booking.status}, timestamp=${booking.timestamp}")

            // Clean up test data
            testRef.removeValue()

            bookingId
        } catch (e: Exception) {
            println("ERROR: Exception in createBooking: ${e.message}")
            println("Exception type: ${e::class.java.simpleName}")
            e.printStackTrace()
            null
        }
    }

    fun getActiveBookings(): Flow<List<FirebaseBooking>> = callbackFlow {
        val listener = bookingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("=== FIREBASE SERVICE ACTIVE BOOKINGS DEBUG ===")
                println("Total bookings in Firebase: ${snapshot.childrenCount}")
                println("Timestamp: ${System.currentTimeMillis()}")

                val bookings = mutableListOf<FirebaseBooking>()
                var processedCount = 0
                var addedCount = 0
                var skippedCount = 0

                snapshot.children.forEach { child ->
                    processedCount++
                    println("\n--- Processing booking #$processedCount: ${child.key} ---")

                    val rawStatus = child.child("status").getValue(String::class.java)
                    val rawCustomerId = child.child("customerId").getValue(String::class.java)
                    println("  Raw status from Firebase: '$rawStatus'")
                    println("  Raw customer ID: '$rawCustomerId'")

                    try {
                        // Try direct conversion first
                        val booking = child.getValue(FirebaseBooking::class.java)
                        if (booking != null) {
                            println("  ✓ Direct conversion SUCCESS for ${booking.id}")
                            println("    - Booking status: '${booking.status}'")
                            println("    - Customer ID: '${booking.customerId}'")
                            println("    - Customer Name: '${booking.customerName}'")
                            println("    - Driver RFID: '${booking.driverRFID}'")
                            println("    - Assigned Driver ID: '${booking.assignedDriverId}'")
                            println("    - Timestamp: ${booking.timestamp}")

                            // Include PENDING, ACCEPTED, AT_PICKUP, IN_PROGRESS, and COMPLETED bookings
                            val allowedStatuses = listOf("PENDING", "ACCEPTED", "AT_PICKUP", "IN_PROGRESS", "COMPLETED")
                            println("    - Checking if status '${booking.status}' is in: $allowedStatuses")

                            if (booking.status in allowedStatuses) {
                                println("    ✓✓✓ ADDING booking to active list: ${booking.id}")
                                bookings.add(booking)
                                addedCount++
                            } else {
                                println("    ✗✗✗ SKIPPING - Status '${booking.status}' not in active list")
                                skippedCount++
                            }
                        } else {
                            println("  ✗ Direct conversion returned NULL for ${child.key}")
                        }
                    } catch (e: Exception) {
                        println("  ✗ Direct conversion FAILED for ${child.key}: ${e.message}")
                        e.printStackTrace()

                        // If direct conversion fails, try manual conversion
                        try {
                            val manualBooking = convertDataSnapshotToFirebaseBooking(child)
                            if (manualBooking != null) {
                                println("  ✓ Manual conversion SUCCESS for ${manualBooking.id}")
                                println("    - Manual booking status: '${manualBooking.status}'")
                                println("    - Manual customer ID: '${manualBooking.customerId}'")

                                val allowedStatuses = listOf("PENDING", "ACCEPTED", "AT_PICKUP", "IN_PROGRESS", "COMPLETED")
                                if (manualBooking.status in allowedStatuses) {
                                    println("    ✓✓✓ ADDING manually converted booking to active list: ${manualBooking.id}")
                                    bookings.add(manualBooking)
                                    addedCount++
                                } else {
                                    println("    ✗✗✗ SKIPPING manual - Status '${manualBooking.status}' not in active list")
                                    skippedCount++
                                }
                            } else {
                                println("  ✗ Manual conversion also returned NULL for ${child.key}")
                            }
                        } catch (e2: Exception) {
                            println("  ✗ Manual conversion also FAILED for ${child.key}: ${e2.message}")
                            android.util.Log.e("FirebaseService", "Failed to convert booking: ${child.key}", e2)
                            e2.printStackTrace()
                        }
                    }
                }

                println("\n=== FIREBASE SERVICE SUMMARY ===")
                println("Total bookings processed: $processedCount")
                println("Bookings added to active list: $addedCount")
                println("Bookings skipped: $skippedCount")
                println("Final active bookings count: ${bookings.size}")

                if (bookings.isNotEmpty()) {
                    println("\n=== ACTIVE BOOKINGS LIST ===")
                    bookings.forEachIndexed { index, booking ->
                        println("[$index] ID: ${booking.id}")
                        println("    Status: ${booking.status}")
                        println("    Customer: ${booking.customerName} (${booking.customerId})")
                        println("    Driver: ${booking.driverName} (RFID: ${booking.driverRFID})")
                    }
                } else {
                    println("⚠️⚠️⚠️ NO ACTIVE BOOKINGS FOUND! ⚠️⚠️⚠️")
                }
                println("===============================================\n")

                trySend(bookings.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                println("❌ Firebase listener cancelled: ${error.message}")
                trySend(emptyList())
            }
        })
        awaitClose { bookingsRef.removeEventListener(listener) }
    }

    private fun convertDataSnapshotToFirebaseBooking(snapshot: DataSnapshot): FirebaseBooking? {
        return try {
            val data = snapshot.value as? Map<String, Any> ?: return null

            FirebaseBooking(
                id = data["id"] as? String ?: snapshot.key ?: "",
                customerId = data["customerId"] as? String ?: "",
                customerName = data["customerName"] as? String ?: "",
                phoneNumber = data["phoneNumber"] as? String ?: "",
                isPhoneVerified = (data["isPhoneVerified"] as? Boolean)
                    ?: (data["phoneVerified"] as? Boolean) ?: false, // Handle both field names for backward compatibility
                pickupLocation = data["pickupLocation"] as? String ?: "",
                destination = data["destination"] as? String ?: "",
                pickupCoordinates = (data["pickupCoordinates"] as? Map<String, Any>)?.mapValues {
                    when (val value = it.value) {
                        is Number -> value.toDouble()
                        is String -> value.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                } ?: emptyMap(),
                dropoffCoordinates = (data["dropoffCoordinates"] as? Map<String, Any>)?.mapValues {
                    when (val value = it.value) {
                        is Number -> value.toDouble()
                        is String -> value.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                } ?: emptyMap(),
                estimatedFare = when (val value = data["estimatedFare"]) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                },
                actualFare = when (val value = data["actualFare"]) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                },
                status = data["status"] as? String ?: "PENDING",
                timestamp = when (val value = data["timestamp"]) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                },
                assignedDriverId = when (val value = data["assignedDriverId"]) {
                    is Number -> value.toString()
                    is String -> value
                    else -> ""
                },
                assignedTricycleId = data["assignedTricycleId"] as? String ?: "",
                verificationCode = data["verificationCode"] as? String ?: "",
                completionTime = when (val value = data["completionTime"]) {
                    is Number -> value.toString()
                    is String -> value
                    else -> "0"
                },
                rating = when (val value = data["rating"]) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                },
                feedback = data["feedback"] as? String ?: "",
                paymentMethod = data["paymentMethod"] as? String ?: "CASH",
                distance = when (val value = data["distance"]) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                },
                duration = when (val value = data["duration"]) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                },
                driverName = data["driverName"] as? String ?: "",
                driverRFID = when (val value = data["driverRFID"]) {
                    is Number -> value.toString()
                    is String -> value
                    else -> ""
                },
                todaNumber = data["todaNumber"] as? String ?: "",
                tripType = data["tripType"] as? String ?: "",
                // Add arrival and no-show tracking fields
                arrivedAtPickup = data["arrivedAtPickup"] as? Boolean ?: false,
                arrivedAtPickupTime = when (val value = data["arrivedAtPickupTime"]) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    else -> 0L
                },
                isNoShow = data["noShow"] as? Boolean ?: false,
                noShowReportedTime = when (val value = data["noShowReportedTime"]) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    else -> 0L
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error in manual conversion", e)
            null
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String, driverId: String? = null): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "bookings/$bookingId/status" to status
            )

            driverId?.let {
                updates["bookings/$bookingId/assignedDriverId"] = it
            }

            // If completing the trip, record completionTime
            if (status == "COMPLETED") {
                updates["bookings/$bookingId/completionTime"] = System.currentTimeMillis()
            }

            // Update bookingIndex status as well
            updates["bookingIndex/$bookingId/status"] = status

            // Update only the specified fields in the existing booking record
            database.updateChildren(updates).await()

            // No longer managing activeBookings index - status in booking is the source of truth

            true
        } catch (e: Exception) {
            false
        }
    }

    // Enhanced updateBookingStatus that auto-creates chat rooms
    suspend fun updateBookingStatusWithChatRoom(bookingId: String, status: String, driverId: String? = null): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "bookings/$bookingId/status" to status
            )

            driverId?.let { userId ->
                // Set the assignedDriverId to the actual driver user ID
                updates["bookings/$bookingId/assignedDriverId"] = userId

                // Fetch the driver's details from the drivers table using the user ID
                try {
                    val driverSnapshot = hardwareDriversRef.child(userId).get().await()

                    if (driverSnapshot.exists()) {
                        // Get driver data from hardware drivers table
                        val driverRFID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""
                        val driverName = driverSnapshot.child("driverName").getValue(String::class.java) ?: ""
                        val todaNumber = driverSnapshot.child("todaNumber").getValue(String::class.java) ?: ""

                        // Set driverRFID to the actual RFID from driver profile
                        if (driverRFID.isNotEmpty()) {
                            updates["bookings/$bookingId/driverRFID"] = driverRFID
                            updates["bookingIndex/$bookingId/driverRFID"] = driverRFID
                        }

                        // Set driver name
                        if (driverName.isNotEmpty()) {
                            updates["bookings/$bookingId/driverName"] = driverName
                        }

                        // Set TODA number (tricycle ID)
                        if (todaNumber.isNotEmpty()) {
                            updates["bookings/$bookingId/assignedTricycleId"] = todaNumber
                            updates["bookings/$bookingId/todaNumber"] = todaNumber
                        }

                        println("✓ Driver assignment: ID=$userId, RFID=$driverRFID, Name=$driverName, TODA=$todaNumber")
                    } else {
                        println("⚠ Driver profile not found in drivers table for ID: $userId")
                        // Fallback: Try users table
                        val userSnapshot = usersRef.child(userId).get().await()
                        val userName = userSnapshot.child("name").getValue(String::class.java) ?: ""
                        if (userName.isNotEmpty()) {
                            updates["bookings/$bookingId/driverName"] = userName
                        }
                        // Set user ID as driverRFID as fallback (legacy behavior)
                        updates["bookings/$bookingId/driverRFID"] = userId
                        updates["bookingIndex/$bookingId/driverRFID"] = userId
                    }
                } catch (e: Exception) {
                    println("⚠ Error fetching driver profile: ${e.message}")
                    // Fallback: set user ID as driverRFID
                    updates["bookings/$bookingId/driverRFID"] = userId
                    updates["bookingIndex/$bookingId/driverRFID"] = userId
                }
            }

            // Update bookingIndex status
            updates["bookingIndex/$bookingId/status"] = status

            // Update the booking
            database.updateChildren(updates).await()

            // If status is IN_PROGRESS and we have a driver, create chat room
            if (status == "IN_PROGRESS" && driverId != null) {
                // Get booking details to create chat room
                val bookingSnapshot = bookingsRef.child(bookingId).get().await()
                val booking = bookingSnapshot.getValue(FirebaseBooking::class.java)

                if (booking != null) {
                    // Get driver details
                    val driverSnapshot = usersRef.child(driverId).get().await()
                    val driver = driverSnapshot.getValue(FirebaseUser::class.java)

                    if (driver != null) {
                        println("Auto-creating chat room for IN_PROGRESS booking")
                        createOrGetChatRoom(
                            bookingId = bookingId,
                            customerId = booking.customerId,
                            customerName = booking.customerName,
                            driverId = driverId,
                            driverName = driver.name
                        )
                    }
                }
            }

            true
        } catch (e: Exception) {
            println("Error updating booking status with chat room: ${e.message}")
            false
        }
    }

    // Driver Location Management
    suspend fun updateDriverLocation(location: FirebaseDriverLocation): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "driverLocations/${location.driverId}" to location,
                "tricycles/${location.tricycleId}/currentLocation" to mapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude
                ),
                "tricycles/${location.tricycleId}/isOnline" to location.isOnline
            )

            // Update available drivers index
            if (location.isAvailable && location.isOnline) {
                updates["availableDrivers/${location.driverId}"] = mapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude,
                    "tricycleId" to location.tricycleId,
                    "timestamp" to location.timestamp
                )
            }

            // Apply updates first
            database.updateChildren(updates).await()

            // Remove from available drivers if not available (separate operation)
            if (!location.isAvailable || !location.isOnline) {
                availableDriversRef.child(location.driverId).removeValue().await()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    // Mark driver as arrived at pickup point
    suspend fun markArrivedAtPickup(bookingId: String): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "bookings/$bookingId/arrivedAtPickup" to true,
                "bookings/$bookingId/arrivedAtPickupTime" to System.currentTimeMillis(),
                "bookingIndex/$bookingId/arrivedAtPickup" to true,
                "bookingIndex/$bookingId/arrivedAtPickupTime" to System.currentTimeMillis()
            )

            database.updateChildren(updates).await()
            println("✓ Marked booking $bookingId as arrived at pickup")
            true
        } catch (e: Exception) {
            println("✗ Error marking arrived at pickup: ${e.message}")
            false
        }
    }

    // Report customer no-show
    suspend fun reportNoShow(bookingId: String): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "bookings/$bookingId/isNoShow" to true,
                "bookings/$bookingId/noShowReportedTime" to System.currentTimeMillis(),
                "bookings/$bookingId/status" to "NO_SHOW",
                "bookingIndex/$bookingId/isNoShow" to true,
                "bookingIndex/$bookingId/noShowReportedTime" to System.currentTimeMillis(),
                "bookingIndex/$bookingId/status" to "NO_SHOW"
            )

            database.updateChildren(updates).await()
            println("✓ Reported no-show for booking $bookingId")
            true
        } catch (e: Exception) {
            println("✗ Error reporting no-show: ${e.message}")
            false
        }
    }

    // Get available drivers
    fun getAvailableDrivers(): Flow<Map<String, Map<String, Any>>> = callbackFlow {
        val listener = availableDriversRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driversMap = mutableMapOf<String, Map<String, Any>>()
                snapshot.children.forEach { child ->
                    child.key?.let { driverId ->
                        @Suppress("UNCHECKED_CAST")
                        val driverData = child.value as? Map<String, Any>
                        if (driverData != null) {
                            driversMap[driverId] = driverData
                        }
                    }
                }
                trySend(driversMap)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        })
        awaitClose { availableDriversRef.removeEventListener(listener) }
    }

    // Driver Management
    suspend fun getDriverById(driverId: String): Map<String, Any>? {
        return try {
            val snapshot = hardwareDriversRef.child(driverId).get().await()
            if (snapshot.exists()) {
                snapshot.value as? Map<String, Any>
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Create driver in drivers table
    suspend fun createDriverInDriversTable(
        userId: String, // ADD: Use Auth User ID as driver ID
        driverName: String,
        todaNumber: String,
        phoneNumber: String,
        address: String,
        emergencyContact: String,
        licenseNumber: String,
        licenseExpiry: Long,
        yearsOfExperience: Int,
        tricyclePlateNumber: String
    ): String? {
        return try {
            // Use the provided userId instead of generating a new push key
            val driverId = userId
            val driverRef = hardwareDriversRef.child(driverId)

            val driver = mapOf(
                "driverId" to driverId,
                "driverName" to driverName,
                "todaNumber" to todaNumber,
                "phoneNumber" to phoneNumber,
                "address" to address,
                "emergencyContact" to emergencyContact,
                "licenseNumber" to licenseNumber,
                "licenseExpiry" to licenseExpiry,
                "yearsOfExperience" to yearsOfExperience,
                "tricyclePlateNumber" to tricyclePlateNumber,
                "rfidUID" to "",
                "isActive" to false,
                "registrationDate" to System.currentTimeMillis(),
                "status" to "PENDING_APPROVAL",
                "hasRfidAssigned" to false,
                "hasTodaMembershipAssigned" to false
            )

            driverRef.setValue(driver).await()
            driverId
        } catch (e: Exception) {
            println("Error creating driver in drivers table: ${e.message}")
            null
        }
    }

    // Get all drivers
    suspend fun getAllDrivers(): List<Map<String, Any>> {
        return try {
            val snapshot = hardwareDriversRef.get().await()
            val drivers = mutableListOf<Map<String, Any>>()
            snapshot.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val driverData = child.value as? Map<String, Any>
                if (driverData != null) {
                    drivers.add(driverData + ("id" to (child.key ?: "")))
                }
            }
            drivers
        } catch (e: Exception) {
            println("Error getting all drivers: ${e.message}")
            emptyList()
        }
    }

    // Assign RFID to driver
    suspend fun assignRfidToDriver(driverId: String, rfidUID: String): Boolean {
        return try {
            val updates = mapOf(
                "drivers/$driverId/rfidUID" to rfidUID,
                "drivers/$driverId/hasRfidAssigned" to true,
                "rfidUIDIndex/$rfidUID" to driverId
            )
            database.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            println("Error assigning RFID to driver: ${e.message}")
            false
        }
    }

    // System Management
    suspend fun updateSystemStatus(component: String, status: FirebaseSystemStatus): Boolean {
        return try {
            systemStatusRef.child(component).setValue(status).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSystemStatus(): Flow<Map<String, FirebaseSystemStatus>> = callbackFlow {
        val listener = systemStatusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusMap = mutableMapOf<String, FirebaseSystemStatus>()
                snapshot.children.forEach { child ->
                    child.key?.let { component ->
                        child.getValue(FirebaseSystemStatus::class.java)?.let { status ->
                            statusMap[component] = status
                        }
                    }
                }
                trySend(statusMap)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        })
        awaitClose { systemStatusRef.removeEventListener(listener) }
    }

    // Unified Queue Management (Combines Hardware + Mobile)
    fun getUnifiedDriverQueue(): Flow<List<UnifiedQueueEntry>> = callbackFlow {
        val listener = driverQueueRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = snapshot.children.mapNotNull {
                        it.getValue(FirebaseDriverQueueEntry::class.java)?.let { entry ->
                            UnifiedQueueEntry(
                                driverId = entry.driverId,
                                driverName = entry.driverName,
                                queuePosition = entry.queuePosition,
                                timestamp = entry.timestamp,
                                source = "hardware",
                                isInPhysicalQueue = true
                            )
                        }
                    }
                    trySend(entries)
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })
        awaitClose { driverQueueRef.removeEventListener(listener) }
    }

    // Driver Contributions Management
    suspend fun getDriverContributions(driverId: String): List<FirebaseContribution> {
        return try {
            println("=== GETTING DRIVER CONTRIBUTIONS ===")
            println("Driver ID: $driverId")

            // First get the driver's RFID
            val driverSnapshot = hardwareDriversRef.child(driverId).get().await()
            val driverRFID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""

            println("Driver RFID: $driverRFID")

            if (driverRFID.isEmpty()) {
                println("No RFID found for driver $driverId")
                return emptyList()
            }

            // Query contributions by driverRFID
            val snapshot = contributionsRef.orderByChild("driverRFID").equalTo(driverRFID).get().await()

            val contributions = mutableListOf<FirebaseContribution>()
            snapshot.children.forEach { child ->
                val amount = child.child("amount").getValue(Double::class.java) ?: 0.0
                val date = child.child("date").getValue(String::class.java) ?: ""
                val timestamp = child.child("timestamp").getValue(String::class.java) ?: "0"
                val driverName = child.child("driverName").getValue(String::class.java) ?: ""
                val todaNumber = child.child("todaNumber").getValue(String::class.java) ?: ""

                // Convert timestamp string to Long
                val timestampLong = try {
                    timestamp.toLong() * 1000 // Convert seconds to milliseconds
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                contributions.add(
                    FirebaseContribution(
                        id = child.key ?: "",
                        driverId = driverId,
                        driverName = driverName,
                        rfidUID = driverRFID,
                        amount = amount,
                        timestamp = timestampLong,
                        date = date,
                        contributionType = "MANUAL",
                        notes = "TODA $todaNumber",
                        verified = true,
                        source = "mobile"
                    )
                )
            }

            println("Found ${contributions.size} contributions for driver $driverId (RFID: $driverRFID)")
            contributions.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            println("Error getting driver contributions: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getDriverTodayContributions(driverId: String): List<FirebaseContribution> {
        return try {
            val todayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            val allContributions = getDriverContributions(driverId)
            allContributions.filter { it.timestamp >= todayStart }
        } catch (e: Exception) {
            println("Error getting driver today contributions: ${e.message}")
            emptyList()
        }
    }

    // Record coin insertion/contribution
    suspend fun recordCoinInsertion(rfidUID: String, amount: Double = 20.0, deviceId: String = "default"): Boolean {
        return try {
            println("=== RECORDING COIN INSERTION ===")
            println("RFID: $rfidUID, Amount: ₱$amount, Device: $deviceId")

            // First, find the driver by RFID
            val driverSnapshot = hardwareDriversRef.orderByChild("rfidUID").equalTo(rfidUID).get().await()

            if (!driverSnapshot.exists()) {
                println("No driver found with RFID: $rfidUID")
                return false
            }

            val driverData = driverSnapshot.children.firstOrNull()?.value as? Map<String, Any>
            val driverId = driverData?.get("driverId") as? String
            val driverName = driverData?.get("driverName") as? String

            if (driverId == null || driverName == null) {
                println("Invalid driver data for RFID: $rfidUID")
                return false
            }

            // Create contribution record
            val contributionRef = contributionsRef.push()
            val contributionId = contributionRef.key ?: return false

            val contribution = FirebaseContribution(
                id = contributionId,
                driverId = driverId,
                driverName = driverName,
                rfidUID = rfidUID,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                contributionType = "COIN_INSERTION",
                notes = "Coin inserted - driver can now receive bookings",
                deviceId = deviceId,
                verified = true
            )

            contributionRef.setValue(contribution).await()

            // Update driver's availability status
            val updates = mapOf(
                "drivers/$driverId/lastContribution" to System.currentTimeMillis(),
                "drivers/$driverId/canReceiveBookings" to true,
                "drivers/$driverId/contributionToday" to true
            )
            database.updateChildren(updates).await()

            println("✅ Coin insertion recorded successfully for driver: $driverName ($driverId)")

            // Trigger a notification or update to all listening apps
            notifyDriverStatusChange(driverId, true)

            true
        } catch (e: Exception) {
            println("❌ Error recording coin insertion: ${e.message}")
            false
        }
    }

    private suspend fun notifyDriverStatusChange(driverId: String, isOnline: Boolean) {
        try {
            // Update a status node that apps can listen to for real-time updates
            val statusUpdate = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "driverId" to driverId,
                "isOnline" to isOnline,
                "reason" to if (isOnline) "COIN_INSERTED" else "END_OF_DAY"
            )

            database.child("driverStatusUpdates").push().setValue(statusUpdate).await()
        } catch (e: Exception) {
            println("Error notifying driver status change: ${e.message}")
        }
    }

    // Method to listen for real-time driver status changes
    fun getDriverStatusUpdates(): Flow<Map<String, Any>> = callbackFlow {
        val listener = database.child("driverStatusUpdates")
            .orderByChild("timestamp")
            .limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.lastOrNull()?.let { child ->
                        val statusUpdate = child.value as? Map<String, Any>
                        if (statusUpdate != null) {
                            trySend(statusUpdate)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        awaitClose { database.child("driverStatusUpdates").removeEventListener(listener) }
    }

    suspend fun fixDriverVerificationStatus(phoneNumber: String): Boolean {
        return try {
            println("=== FIXING DRIVER VERIFICATION STATUS ===")
            println("Phone number: $phoneNumber")

            // Get the user ID from phone number index
            val userId = phoneNumberIndexRef.child(phoneNumber).get().await().getValue(String::class.java)

            if (userId != null) {
                println("Found user ID: $userId")

                // Get the current user record
                val currentUser = usersRef.child(userId).get().await().getValue(FirebaseUser::class.java)

                if (currentUser != null && currentUser.userType == "DRIVER") {
                    println("Current user: ${currentUser.name}, isVerified: ${currentUser.isVerified}")

                    // Update the isVerified field to true
                    usersRef.child(userId).child("isVerified").setValue(true).await()

                    // Add a small delay to ensure Firebase updates are propagated
                    kotlinx.coroutines.delay(500)

                    // Verify the update was successful
                    val updatedUser = usersRef.child(userId).get().await().getValue(FirebaseUser::class.java)
                    if (updatedUser != null && updatedUser.isVerified) {
                        println("Successfully updated and verified isVerified to true for driver: ${currentUser.name}")
                        true
                    } else {
                        println("Update failed - verification status still false")
                        false
                    }
                } else {
                    println("User not found or not a driver")
                    false
                }
            } else {
                println("No user ID found for phone number: $phoneNumber")
                false
            }
        } catch (e: Exception) {
            println("Error fixing driver verification status: ${e.message}")
            false
        }
    }

    suspend fun getDriverApplicationStatus(phoneNumber: String): String? {
        return try {
            println("=== CHECKING DRIVER APPLICATION STATUS ===")
            println("Phone number: $phoneNumber")

            // First, check if driver exists in the drivers table (new approach)
            val driversSnapshot = hardwareDriversRef.orderByChild("phoneNumber").equalTo(phoneNumber).get().await()

            if (driversSnapshot.exists()) {
                val driver = driversSnapshot.children.firstOrNull()?.getValue<Map<String, Any>>()
                if (driver != null) {
                    val isActive = driver["isActive"] as? Boolean ?: false
                    println("Found driver in drivers table for $phoneNumber: isActive = $isActive")
                    return if (isActive) "APPROVED" else "PENDING"
                }
            }

            // Fallback: Search for driver application by phone number in driverRegistrations (legacy approach)
            val snapshot = driverRegistrationsRef.orderByChild("phoneNumber").equalTo(phoneNumber).get().await()

            if (snapshot.exists()) {
                val application = snapshot.children.firstOrNull()?.getValue(FirebaseDriverRegistration::class.java)
                println("Found application for $phoneNumber: Status = ${application?.status}")
                application?.status
            } else {
                println("No driver application found for phone number: $phoneNumber")
                null
            }
        } catch (e: Exception) {
            println("Error checking driver application status: ${e.message}")
            null
        }
    }

    suspend fun getDriverContributionStatus(driverId: String): Boolean {
        return try {
            println("=== CHECKING DRIVER CONTRIBUTION STATUS ===")
            println("Driver ID: $driverId")

            // Check if driver has made any contributions today
            val todayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            val snapshot = contributionsRef.orderByChild("driverId").equalTo(driverId).get().await()

            var hasContributedToday = false
            snapshot.children.forEach { child ->
                val contribution = child.getValue(FirebaseContribution::class.java)
                if (contribution != null && contribution.timestamp >= todayStart) {
                    hasContributedToday = true
                    return@forEach
                }
            }

            println("Driver $driverId contribution status: $hasContributedToday")
            hasContributedToday
        } catch (e: Exception) {
            println("Error checking driver contribution status: ${e.message}")
            false
        }
    }

    suspend fun isDriverInQueue(driverRFID: String): Boolean {
        return try {
            println("=== CHECKING IF DRIVER IS IN QUEUE ===")
            println("Driver RFID: $driverRFID")

            if (driverRFID.isEmpty()) {
                println("Driver RFID is empty, returning false")
                return false
            }

            // Check the queue table for this driver's RFID
            val queueRef = database.child("queue")
            val snapshot = queueRef.get().await()

            var isInQueue = false
            snapshot.children.forEach { child ->
                val queueDriverRFID = child.child("driverRFID").getValue(String::class.java) ?: ""
                val status = child.child("status").getValue(String::class.java) ?: ""

                if (queueDriverRFID == driverRFID && status == "waiting") {
                    isInQueue = true
                    println("✓ Driver $driverRFID found in queue with status: $status")
                    return@forEach
                }
            }

            println("Driver $driverRFID in queue status: $isInQueue")
            isInQueue
        } catch (e: Exception) {
            println("Error checking if driver is in queue: ${e.message}")
            false
        }
    }

    // Real-time observer for driver queue status
    fun observeDriverQueueStatus(driverRFID: String): Flow<Boolean> = callbackFlow {
        println("=== STARTING REAL-TIME QUEUE OBSERVER ===")
        println("Observing queue for driver RFID: $driverRFID")

        if (driverRFID.isEmpty()) {
            println("Driver RFID is empty, emitting false")
            trySend(false).isSuccess
            close()
            return@callbackFlow
        }

        // First, get the driver's name and ID to match by multiple criteria
        var driverName = ""
        var driverId = ""

        try {
            val driverSnapshot = hardwareDriversRef.orderByChild("rfidUID").equalTo(driverRFID).get().await()
            if (driverSnapshot.exists()) {
                val driverData = driverSnapshot.children.firstOrNull()?.value as? Map<String, Any>
                driverName = driverData?.get("driverName") as? String ?: ""
                driverId = driverData?.get("driverId") as? String ?: ""
                println("Found driver: name=$driverName, id=$driverId")
            }
        } catch (e: Exception) {
            println("Error getting driver info: ${e.message}")
        }

        val queueRef = database.child("queue")

        val listener = queueRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("=== QUEUE DATA CHANGED ===")
                println("Queue snapshot exists: ${snapshot.exists()}")
                println("Queue children count: ${snapshot.childrenCount}")

                var isInQueue = false
                snapshot.children.forEach { child ->
                    // Safely handle both String and Number types for RFID
                    val queueDriverRFID = when (val rfidValue = child.child("driverRFID").value) {
                        is String -> rfidValue
                        is Number -> rfidValue.toString()
                        else -> ""
                    }
                    val queueDriverName = child.child("driverName").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: ""

                    println("  Checking queue entry: key=${child.key}, driverRFID=$queueDriverRFID, driverName=$queueDriverName, status=$status")

                    // Match by CURRENT RFID OR driver name (to handle RFID changes)
                    val rfidMatches = queueDriverRFID == driverRFID
                    val nameMatches = driverName.isNotEmpty() && queueDriverName.equals(driverName, ignoreCase = true)

                    if ((rfidMatches || nameMatches) && status == "waiting") {
                        isInQueue = true
                        if (rfidMatches) {
                            println("✓✓✓ Driver $driverRFID IS in queue (matched by RFID) with status: $status ✓✓✓")
                        } else {
                            println("✓✓✓ Driver $driverName IS in queue (matched by NAME) with status: $status ✓✓✓")
                            println("    NOTE: Queue has old RFID ($queueDriverRFID), driver's current RFID is $driverRFID")
                        }
                        return@forEach
                    }
                }
                println("=== EMITTING QUEUE STATUS: $isInQueue for driver $driverRFID ($driverName) ===")
                val result = trySend(isInQueue)
                if (result.isFailure) {
                    println("ERROR: Failed to emit queue status: ${result.exceptionOrNull()}")
                } else {
                    println("SUCCESS: Emitted queue status: $isInQueue")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Queue observer cancelled: ${error.message}")
                trySend(false).isSuccess
                close(error.toException())
            }
        })

        awaitClose {
            println("Removing queue observer for driver $driverRFID")
            queueRef.removeEventListener(listener)
        }
    }

    fun observeDriverQueue(): Flow<List<QueueEntry>> = callbackFlow {
        println("=== STARTING REAL-TIME QUEUE LIST OBSERVER ===")

        val queueRef = database.child("queue")
        val listener = queueRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("=== QUEUE LIST DATA CHANGED ===")
                println("Queue snapshot exists: ${snapshot.exists()}")
                println("Queue children count: ${snapshot.childrenCount}")

                val queueEntries = mutableListOf<QueueEntry>()

                snapshot.children.forEachIndexed { index, child ->
                    try {
                        println("Processing queue entry: ${child.key}")
                        val driverRFID = child.child("driverRFID").getValue(String::class.java) ?: ""
                        val driverName = child.child("driverName").getValue(String::class.java) ?: ""
                        val status = child.child("status").getValue(String::class.java) ?: ""

                        // Handle timestamp - try queueTime first (Unix timestamp in seconds as string), then timestamp field
                        val queueTimeStr = child.child("queueTime").getValue(String::class.java)
                        val timestampStr = child.child("timestamp").getValue(String::class.java)

                        val timestamp = when {
                            queueTimeStr != null -> {
                                try {
                                    queueTimeStr.toLong() * 1000 // Convert seconds to milliseconds
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                            }
                            timestampStr != null -> {
                                // Try parsing the date string "2025-10-06 16:40:44"
                                try {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    sdf.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                            }
                            else -> {
                                // Try getting as Long directly
                                child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                            }
                        }

                        println("  driverRFID: $driverRFID")
                        println("  driverName: $driverName")
                        println("  status: $status")
                        println("  timestamp: $timestamp")

                        if (status == "waiting" && driverRFID.isNotEmpty()) {
                            val queueEntry = QueueEntry(
                                driverRFID = driverRFID,
                                driverName = driverName,
                                timestamp = timestamp,
                                position = index + 1
                            )
                            queueEntries.add(queueEntry)
                            println("✓ Added queue entry: $driverName ($driverRFID) at position ${index + 1}")
                        } else {
                            println("✗ Skipped entry - status: $status, RFID empty: ${driverRFID.isEmpty()}")
                        }
                    } catch (e: Exception) {
                        println("Error parsing queue entry ${child.key}: ${e.message}")
                        e.printStackTrace()
                    }
                }

                println("Total queue entries found: ${queueEntries.size}")
                trySend(queueEntries)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Queue list observer cancelled: ${error.message}")
                trySend(emptyList())
            }
        })

        awaitClose {
            println("Removing queue list observer")
            queueRef.removeEventListener(listener)
        }
    }

    suspend fun leaveQueue(driverRFID: String): Boolean {
        return try {
            println("=== LEAVING QUEUE ===")
            println("Driver RFID: $driverRFID")

            if (driverRFID.isEmpty()) {
                println("Driver RFID is empty, cannot leave queue")
                return false
            }

            // Find and delete the queue entry for this driver
            val queueRef = database.child("queue")
            val snapshot = queueRef.get().await()

            println("Found ${snapshot.childrenCount} entries in queue")

            var deleted = false
            for (child in snapshot.children) {
                val queueDriverRFID = child.child("driverRFID").getValue(String::class.java) ?: ""
                println("Checking queue entry: key=${child.key}, driverRFID=$queueDriverRFID")

                if (queueDriverRFID == driverRFID) {
                    val queueKey = child.key
                    if (queueKey != null) {
                        println("Found matching entry, deleting key: $queueKey")
                        queueRef.child(queueKey).removeValue().await()
                        println("✓ Driver $driverRFID removed from queue (key: $queueKey)")
                        deleted = true
                        break
                    }
                }
            }

            if (!deleted) {
                println("✗ Driver $driverRFID was not found in queue")
            }

            deleted
        } catch (e: Exception) {
            println("❌ Error leaving queue: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun getDriverTodayStats(driverId: String): Triple<Int, Double, Double> {
        return try {
            println("=== GETTING DRIVER TODAY STATS ===")
            println("Driver ID: $driverId")

            // First get the driver's RFID
            val driverSnapshot = hardwareDriversRef.child(driverId).get().await()
            val driverRFID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""

            println("Driver RFID: $driverRFID")

            val todayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Get all bookings
            val bookingsSnapshot = bookingsRef.get().await()

            var todayTrips = 0
            var todayEarnings = 0.0

            bookingsSnapshot.children.forEach { child ->
                val status = child.child("status").getValue(String::class.java) ?: ""
                val bookingDriverRFID = child.child("driverRFID").getValue(String::class.java) ?: ""
                val assignedDriverId = child.child("assignedDriverId").getValue(String::class.java) ?: ""
                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                // Parse completionTime which may be stored as String or Number
                val completionTimeAny = child.child("completionTime").value
                val completionTime: Long = when (completionTimeAny) {
                    is Number -> completionTimeAny.toLong()
                    is String -> completionTimeAny.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val effectiveTime = if (completionTime > 0L) completionTime else timestamp

                // Check if this booking belongs to the driver (by RFID or driver ID)
                val isMyBooking = (bookingDriverRFID == driverRFID && driverRFID.isNotEmpty()) ||
                                 (assignedDriverId == driverRFID && driverRFID.isNotEmpty()) ||
                                 (assignedDriverId == driverId)

                if (isMyBooking && status == "COMPLETED" && effectiveTime >= todayStart) {
                    todayTrips++

                    // Get fare (prefer actualFare, fallback to estimatedFare)
                    val actualFare = when (val v = child.child("actualFare").value) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val estimatedFare = when (val v = child.child("estimatedFare").value) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val fare = if (actualFare > 0.0) actualFare else estimatedFare

                    todayEarnings += fare

                    println("Found completed trip: fare=₱$fare, effectiveTime=$effectiveTime")
                }
            }

            // Compute today's average rating for this driver
            var driverRating = -1.0 // Sentinel for "no ratings today"
            try {
                val ratingsSnapshot = ratingsRef.orderByChild("driverId").equalTo(driverId).get().await()
                var sumStars = 0.0
                var count = 0
                ratingsSnapshot.children.forEach { child ->
                    val tsAny = child.child("timestamp").value
                    val timestamp = when (tsAny) {
                        is Number -> tsAny.toLong()
                        is String -> tsAny.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    if (timestamp < todayStart) return@forEach

                    val starsAny = child.child("stars").value
                    val stars = when (starsAny) {
                        is Number -> starsAny.toInt()
                        is String -> starsAny.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val ratedBy = child.child("ratedBy").getValue(String::class.java) ?: ""

                    // Count only customer-submitted ratings with stars > 0
                    if (stars > 0 && ratedBy.equals("CUSTOMER", ignoreCase = true)) {
                        sumStars += stars
                        count++
                    }
                }
                if (count > 0) {
                    driverRating = sumStars / count
                }
            } catch (e: Exception) {
                println("Error computing today's ratings: ${e.message}")
            }

            println("Today's stats: $todayTrips trips, ₱$todayEarnings earnings, rating=${if (driverRating < 0) "--" else driverRating}")

            Triple(todayTrips, todayEarnings, driverRating)
        } catch (e: Exception) {
            println("Error getting driver today stats: ${e.message}")
            e.printStackTrace()
            Triple(0, 0.0, -1.0)
        }
    }

    // Emergency Management
    suspend fun createEmergencyAlert(alert: FirebaseEmergencyAlert): String? {
        return try {
            val alertRef = emergencyAlertsRef.push()
            val alertId = alertRef.key ?: return null
            val alertWithId = alert.copy(id = alertId)
            alertRef.setValue(alertWithId).await()
            alertId
        } catch (e: Exception) {
            println("Error creating emergency alert: ${e.message}")
            null
        }
    }

    // Rating Management
    suspend fun createRatingEntry(bookingId: String): Boolean {
        return try {
            println("=== CREATING RATING ENTRY ===")
            println("Booking ID: $bookingId")

            // Get booking details
            val bookingSnapshot = bookingsRef.child(bookingId).get().await()
            val booking = bookingSnapshot.getValue(FirebaseBooking::class.java)

            if (booking == null) {
                println("✗ Booking not found: $bookingId")
                return false
            }

            // Create rating entry with initial values
            val ratingRef = ratingsRef.push()
            val ratingId = ratingRef.key ?: return false

            val rating = FirebaseRating(
                id = ratingId,
                bookingId = bookingId,
                customerId = booking.customerId,
                customerName = booking.customerName,
                driverId = booking.assignedDriverId,
                driverName = booking.driverName,
                stars = 0, // Default, can be updated later
                feedback = "", // Default, can be updated later
                timestamp = System.currentTimeMillis(),
                ratedBy = "DRIVER"
            )

            ratingRef.setValue(rating).await()
            println("✓ Rating entry created: $ratingId for booking $bookingId")
            true
        } catch (e: Exception) {
            println("✗ Error creating rating entry: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun updateRating(bookingId: String, stars: Int, feedback: String): Boolean {
        return try {
            println("=== UPDATING RATING ===")
            println("Booking ID: $bookingId, Stars: $stars")

            // Find the rating entry for this booking
            val ratingSnapshot = ratingsRef.orderByChild("bookingId").equalTo(bookingId).get().await()

            if (!ratingSnapshot.exists()) {
                println("✗ No rating entry found for booking: $bookingId")
                return false
            }

            // Get the first (and should be only) rating entry
            val ratingKey = ratingSnapshot.children.firstOrNull()?.key

            if (ratingKey != null) {
                val updates = mapOf(
                    "ratings/$ratingKey/stars" to stars,
                    "ratings/$ratingKey/feedback" to feedback,
                    "ratings/$ratingKey/timestamp" to System.currentTimeMillis(),
                    "ratings/$ratingKey/ratedBy" to "CUSTOMER"
                )
                database.updateChildren(updates).await()
                println("✓ Rating updated successfully")
                true
            } else {
                println("✗ Rating key not found")
                false
            }
        } catch (e: Exception) {
            println("✗ Error updating rating: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Assign the first driver in queue to a booking immediately upon creation
    suspend fun matchBookingToFirstDriver(bookingId: String): Boolean {
        return try {
            // Ensure booking is still PENDING before attempting assignment
            val currentStatus = bookingsRef.child(bookingId).child("status").get().await().getValue(String::class.java)
            if (currentStatus != null && currentStatus != "PENDING") {
                println("Booking $bookingId is $currentStatus; skipping auto-match")
                return false
            }

            // Read queue snapshot
            val queueRef = database.child("queue")
            val queueSnapshot = queueRef.get().await()
            if (!queueSnapshot.exists() || !queueSnapshot.hasChildren()) {
                println("No drivers in queue to match for booking $bookingId")
                return false
            }

            // Find all queue entries sorted by timestamp (oldest first)
            val sortedEntries = queueSnapshot.children
                .mapNotNull { child ->
                    val key = child.key ?: return@mapNotNull null
                    val ts = key.toLongOrNull() ?: return@mapNotNull null
                    ts to child
                }
                .sortedBy { it.first }

            if (sortedEntries.isEmpty()) {
                println("Queue has no valid timestamp keys; cannot match")
                return false
            }

            // Try each queue entry until we find one that's available
            for ((timestamp, queueEntry) in sortedEntries) {
                val queueKey = queueEntry.key ?: continue

                // Check if already claimed
                val alreadyClaimed = queueEntry.child("claimed").getValue(Boolean::class.java) ?: false
                val queueStatus = queueEntry.child("status").getValue(String::class.java) ?: "waiting"

                println("Checking queue entry $queueKey: claimed=$alreadyClaimed, status=$queueStatus")

                // Skip if claimed and status is not "waiting" (means actively working on a booking)
                if (alreadyClaimed && queueStatus != "waiting") {
                    println("Queue entry $queueKey is claimed and busy; skipping")
                    continue
                }

                // If claimed but status is "waiting", reset the claim (stale claim from previous booking)
                if (alreadyClaimed && queueStatus == "waiting") {
                    println("Queue entry $queueKey has stale claim; resetting")
                    queueRef.child(queueKey).child("claimed").setValue(false).await()
                }

                // Try to atomically claim this entry
                val claimRef = queueRef.child(queueKey).child("claimed")
                var claimed = false
                var transactionComplete = false

                claimRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val v = mutableData.getValue(Boolean::class.java) ?: false
                        return if (!v) {
                            mutableData.value = true
                            Transaction.success(mutableData)
                        } else {
                            Transaction.abort()
                        }
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        claimed = committed && (currentData?.getValue(Boolean::class.java) == true)
                        transactionComplete = true
                    }
                })

                // Wait for transaction to complete
                var waitCount = 0
                while (!transactionComplete && waitCount < 20) {
                    kotlinx.coroutines.delay(10)
                    waitCount++
                }

                if (!claimed) {
                    println("Failed to claim queue entry $queueKey; trying next")
                    continue
                }

                // Successfully claimed! Re-check booking still PENDING
                val statusAfterClaim = bookingsRef.child(bookingId).child("status").get().await().getValue(String::class.java)
                if (statusAfterClaim != null && statusAfterClaim != "PENDING") {
                    println("Booking $bookingId changed to $statusAfterClaim after claim; releasing entry")
                    claimRef.setValue(false).await()
                    return false
                }

                // Get driver details from this entry
                val driverName = queueEntry.child("driverName").getValue(String::class.java) ?: ""
                val todaNumber = queueEntry.child("todaNumber").getValue(String::class.java) ?: ""
                val driverRFID = queueEntry.child("driverRFID").getValue(String::class.java) ?: ""

                if (driverRFID.isBlank()) {
                    println("Queue entry $queueKey missing driverRFID; releasing claim and trying next")
                    claimRef.setValue(false).await()
                    continue
                }

                // Look up the driver's user ID from their RFID
                var driverUserId = ""
                try {
                    // 1) Fast-path: rfidUIDIndex (populated by assignRfidToDriver)
                    try {
                        val idx = database.child("rfidUIDIndex").child(driverRFID).get().await()
                        val fromIndex = idx.getValue(String::class.java) ?: ""
                        if (fromIndex.isNotEmpty()) {
                            driverUserId = fromIndex
                            println("✓ Found driver user ID from rfidUIDIndex: $driverUserId for RFID: $driverRFID")
                        }
                    } catch (e: Exception) {
                        println("⚠ rfidUIDIndex lookup failed: ${e.message}")
                    }

                    // 2) Drivers table by RFID (if not resolved by index)
                    if (driverUserId.isEmpty()) {
                        val driversSnapshot = hardwareDriversRef.orderByChild("rfidUID").equalTo(driverRFID).get().await()
                        if (driversSnapshot.exists()) {
                            // The user ID is the key of the driver entry
                            val driverEntry = driversSnapshot.children.firstOrNull()
                            driverUserId = driverEntry?.key ?: ""
                            println("✓ Found driver user ID from drivers table: $driverUserId for RFID: $driverRFID")
                        }
                    }

                    // 3) Users table by rfidNumber (legacy schema)
                    if (driverUserId.isEmpty()) {
                        val usersSnapshot = usersRef.orderByChild("rfidNumber").equalTo(driverRFID).get().await()
                        if (usersSnapshot.exists()) {
                            val userEntry = usersSnapshot.children.firstOrNull()
                            driverUserId = userEntry?.key ?: ""
                            println("✓ Found driver user ID from users table (rfidNumber): $driverUserId for RFID: $driverRFID")
                        }
                    }

                    // 4) Drivers table by todaNumber (as last-ditch structured hint from queue)
                    if (driverUserId.isEmpty() && todaNumber.isNotEmpty()) {
                        try {
                            val byToda = hardwareDriversRef.orderByChild("todaNumber").equalTo(todaNumber).get().await()
                            if (byToda.exists()) {
                                val driverEntry = byToda.children.firstOrNull()
                                driverUserId = driverEntry?.key ?: ""
                                println("✓ Found driver user ID from drivers table (todaNumber=$todaNumber): $driverUserId")
                            }
                        } catch (e: Exception) {
                            println("⚠ todaNumber lookup failed: ${e.message}")
                        }
                    }

                    if (driverUserId.isNotEmpty()) {
                        println("✓ Successfully resolved driver user ID: $driverUserId for RFID: $driverRFID")
                    } else {
                        println("⚠ Could not find driver user ID for RFID: $driverRFID (todaNumber='$todaNumber', driverName='$driverName')")
                    }
                } catch (e: Exception) {
                    println("⚠ Error looking up driver user ID: ${e.message}")
                }

                // If we couldn't find the user ID, use the RFID as fallback (legacy behavior)
                if (driverUserId.isEmpty()) {
                    println("⚠ Could not find user ID for RFID $driverRFID, using RFID as fallback")
                    driverUserId = driverRFID
                }

                // Prepare updates for booking and index
                val updates = mutableMapOf<String, Any>(
                    "bookings/$bookingId/assignedDriverId" to driverUserId,  // ✅ Prefer actual user ID; falls back to RFID only if unresolved
                    "bookings/$bookingId/driverRFID" to driverRFID,
                    "bookings/$bookingId/driverName" to driverName,
                    "bookings/$bookingId/assignedTricycleId" to todaNumber,
                    "bookings/$bookingId/todaNumber" to todaNumber,
                    "bookings/$bookingId/status" to "ACCEPTED",
                    "bookingIndex/$bookingId/status" to "ACCEPTED",
                    "bookingIndex/$bookingId/driverRFID" to driverRFID
                )

                // Apply updates atomically and remove the queue entry
                database.updateChildren(updates).await()
                // Remove the queue entry now that it's assigned
                queueRef.child(queueKey).removeValue().await()

                println("✅ Matched booking $bookingId with driverUserId=$driverUserId, driverRFID=$driverRFID, driverName=$driverName, todaNumber=$todaNumber")
                return true
            }

            println("No available drivers in queue for booking $bookingId (all entries claimed/busy)")
            false
        } catch (e: Exception) {
            println("Error matching booking to first driver: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Get booking by ID (non-streaming, single fetch)
    suspend fun getBookingById(bookingId: String): FirebaseBooking? {
        return try {
            val snapshot = bookingsRef.child(bookingId).get().await()
            if (snapshot.exists()) {
                snapshot.getValue(FirebaseBooking::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error getting booking by ID: ${e.message}")
            null
        }
    }

    // Try to match booking to first driver in queue (wrapper for matchBookingToFirstDriver)
    suspend fun tryMatchBookingToFirstDriver(bookingId: String): Boolean {
        return matchBookingToFirstDriver(bookingId)
    }

    // Report missing RFID for a driver
    suspend fun reportMissingRfid(driverId: String): Boolean {
        return try {
            println("=== REPORTING MISSING RFID ===")
            println("Driver ID: $driverId")

            // Get driver details first
            val driverSnapshot = hardwareDriversRef.child(driverId).get().await()
            if (!driverSnapshot.exists()) {
                println("✗ Driver not found: $driverId")
                return false
            }

            val driverName = driverSnapshot.child("driverName").getValue(String::class.java) ?: ""
            // Support both rfidUID and rfidNumber fields
            val currentRFID = driverSnapshot.child("rfidUID").getValue(String::class.java)
                ?: driverSnapshot.child("rfidNumber").getValue(String::class.java)
                ?: ""

            if (currentRFID.isEmpty()) {
                println("✗ Driver has no RFID assigned")
                return false
            }

            println("Driver: $driverName, Current RFID: $currentRFID")

            // Add a record to the rfidChangeHistory
            val historyRef = rfidChangeHistoryRef.push()
            val historyId = historyRef.key ?: return false

            val historyEntry = mapOf(
                "id" to historyId,
                "driverId" to driverId,
                "driverName" to driverName,
                "oldRfidUID" to currentRFID,
                "newRfidUID" to "", // Empty because RFID is being unlinked
                "changeType" to "REPORTED_MISSING",
                "reason" to "Driver reported RFID as missing/lost",
                "changedBy" to driverId,
                "changedByName" to driverName,
                "timestamp" to System.currentTimeMillis(),
                "notes" to "Auto-unlinked due to missing report"
            )

            historyRef.setValue(historyEntry).await()

            // ⚠️ AUTO-UNLINK: Remove RFID from driver
            val updates = mutableMapOf<String, Any?>(
                // Clear both RFID fields
                "drivers/$driverId/rfidUID" to "",
                "drivers/$driverId/rfidNumber" to "",

                // Set missing flags
                "drivers/$driverId/rfidMissing" to true,
                "drivers/$driverId/rfidReported" to true,

                // Save old RFID for reference
                "drivers/$driverId/oldRfidUID" to currentRFID,
                "drivers/$driverId/rfidReportedMissingAt" to System.currentTimeMillis(),

                // Update assignment flags
                "drivers/$driverId/hasRfidAssigned" to false,
                "drivers/$driverId/needsRfidAssignment" to true,
                "drivers/$driverId/rfidStatus" to "missing",
                "drivers/$driverId/lastRfidChange" to System.currentTimeMillis(),

                // Remove from RFID index
                "rfidUIDIndex/$currentRFID" to null
            )

            database.updateChildren(updates).await()

            // Create admin notification
            val notificationRef = notificationsRef.push()
            val notificationId = notificationRef.key ?: ""

            val notification = mapOf(
                "id" to notificationId,
                "type" to "RFID_MISSING",
                "title" to "RFID Reported Missing",
                "message" to "$driverName (ID: $driverId) reported RFID $currentRFID as missing. RFID has been auto-unlinked.",
                "driverId" to driverId,
                "driverName" to driverName,
                "oldRfidUID" to currentRFID,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "priority" to "high",
                "actionRequired" to true
            )

            notificationRef.setValue(notification).await()

            println("✓ RFID reported missing and auto-unlinked successfully")
            println("✓ Admin notification created")
            println("✓ History entry created: $historyId")
            println("✓ Fields updated: rfidMissing=true, rfidReported=true, oldRfidUID=$currentRFID")
            true
        } catch (e: Exception) {
            println("✗ Error reporting missing RFID: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Get RFID change history for a driver
    fun getRfidChangeHistory(driverId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = rfidChangeHistoryRef.orderByChild("driverId").equalTo(driverId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val history = snapshot.children.mapNotNull {
                        @Suppress("UNCHECKED_CAST")
                        it.value as? Map<String, Any>
                    }
                    trySend(history)
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })
        awaitClose { rfidChangeHistoryRef.removeEventListener(listener) }
    }

    // Get admin notifications
    fun getAdminNotifications(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = notificationsRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = snapshot.children.mapNotNull {
                        @Suppress("UNCHECKED_CAST")
                        it.value as? Map<String, Any>
                    }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                    trySend(notifications)
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })
        awaitClose { notificationsRef.removeEventListener(listener) }
    }

    // Mark notification as read
    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            notificationsRef.child(notificationId).child("isRead").setValue(true).await()
            true
        } catch (e: Exception) {
            println("Error marking notification as read: ${e.message}")
            false
        }
    }

    // Chat Management
    suspend fun createOrGetChatRoom(
        bookingId: String,
        customerId: String,
        customerName: String,
        driverId: String,
        driverName: String
    ): String? {
        return try {
            // Check if chat room already exists
            val existingRoom = chatRoomsRef.child(bookingId).get().await()
            if (existingRoom.exists()) {
                return bookingId
            }

            // Create new chat room
            val chatRoom = FirebaseChatRoom(
                id = bookingId,
                bookingId = bookingId,
                customerId = customerId,
                customerName = customerName,
                driverId = driverId,
                driverName = driverName,
                createdAt = System.currentTimeMillis(),
                lastMessageTime = System.currentTimeMillis(),
                lastMessage = "",
                isActive = true
            )

            chatRoomsRef.child(bookingId).setValue(chatRoom).await()
            bookingId
        } catch (e: Exception) {
            println("Error creating chat room: ${e.message}")
            null
        }
    }

    suspend fun sendMessage(message: FirebaseChatMessage): Boolean {
        return try {
            val messageRef = chatMessagesRef.push()
            val messageId = messageRef.key ?: return false
            val messageWithId = message.copy(id = messageId)
            messageRef.setValue(messageWithId).await()
            true
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
            false
        }
    }

    suspend fun updateChatRoomLastMessage(bookingId: String, message: String): Boolean {
        return try {
            val updates = mapOf(
                "chatRooms/$bookingId/lastMessage" to message,
                "chatRooms/$bookingId/lastMessageTime" to System.currentTimeMillis()
            )
            database.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            println("Error updating chat room: ${e.message}")
            false
        }
    }

    fun getChatMessages(bookingId: String): Flow<List<FirebaseChatMessage>> = callbackFlow {
        val listener = chatMessagesRef.orderByChild("bookingId").equalTo(bookingId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull {
                        it.getValue(FirebaseChatMessage::class.java)
                    }.sortedBy { it.timestamp }
                    trySend(messages)
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })
        awaitClose { chatMessagesRef.removeEventListener(listener) }
    }

    fun getChatRoom(bookingId: String): Flow<FirebaseChatRoom?> = callbackFlow {
        val listener = chatRoomsRef.child(bookingId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatRoom = snapshot.getValue(FirebaseChatRoom::class.java)
                trySend(chatRoom)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        })
        awaitClose { chatRoomsRef.child(bookingId).removeEventListener(listener) }
    }

    fun getUserChatRooms(userId: String): Flow<List<FirebaseChatRoom>> = callbackFlow {
        // Get chat rooms where user is either customer or driver
        val customerListener = chatRoomsRef.orderByChild("customerId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val customerRooms = snapshot.children.mapNotNull {
                        it.getValue(FirebaseChatRoom::class.java)
                    }

                    // Also get rooms where user is the driver
                    chatRoomsRef.orderByChild("driverId").equalTo(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(driverSnapshot: DataSnapshot) {
                                val driverRooms = driverSnapshot.children.mapNotNull {
                                    it.getValue(FirebaseChatRoom::class.java)
                                }

                                // Combine and deduplicate
                                val allRooms = (customerRooms + driverRooms).distinctBy { it.id }
                                    .sortedByDescending { it.lastMessageTime }
                                trySend(allRooms)
                            }
                            override fun onCancelled(error: DatabaseError) {
                                trySend(emptyList())
                            }
                        })
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })
        awaitClose { chatRoomsRef.removeEventListener(customerListener) }
    }

}
