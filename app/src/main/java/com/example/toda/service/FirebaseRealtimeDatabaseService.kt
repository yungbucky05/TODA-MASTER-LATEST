package com.example.toda.service

import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
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
    private val rfidChangeHistoryRef = database.child("rfidHistory") // FIXED: Changed from rfidChangeHistory to rfidHistory

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

    // New: fetch stored email by phone number (from users node)
    suspend fun getUserEmailByPhoneNumber(phoneNumber: String): String? {
        return try {
            val userId = phoneNumberIndexRef.child(phoneNumber).get().await().getValue(String::class.java)
            if (userId != null) {
                val snapshot = usersRef.child(userId).get().await()
                snapshot.child("email").getValue(String::class.java)
            } else null
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

    // Observe FareMatrix for regular trips
    fun observeRegularFareMatrix(): Flow<FareMatrix?> = callbackFlow {
        val fareMatrixRef = database.child("fareMatrix").child("regular")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val baseFare = when (val value = snapshot.child("baseFare").value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 8.0
                    else -> 8.0
                }
                val perKmRate = when (val value = snapshot.child("perKmRate").value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 2.0
                    else -> 2.0
                }
                val lastUpdated = when (val value = snapshot.child("lastUpdated").value) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val updatedBy = snapshot.child("updatedBy").getValue(String::class.java) ?: ""

                val fareMatrix = FareMatrix(
                    baseFare = baseFare,
                    perKmRate = perKmRate,
                    lastUpdated = lastUpdated,
                    updatedBy = updatedBy
                )
                trySend(fareMatrix)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        fareMatrixRef.addValueEventListener(listener)
        awaitClose { fareMatrixRef.removeEventListener(listener) }
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
                bookingApp = data["bookingApp"] as? String ?: "",
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

            driverId?.let { userId ->
                updates["bookings/$bookingId/assignedDriverId"] = userId

                // Fetch driver details including payment mode
                try {
                    val driverSnapshot = hardwareDriversRef.child(userId).get().await()
                    if (driverSnapshot.exists()) {
                        val paymentMode = driverSnapshot.child("paymentMode").getValue(String::class.java) ?: "pay_every_trip"
                        val driverRFID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""
                        val driverName = driverSnapshot.child("driverName").getValue(String::class.java) ?: ""
                        val todaNumber = driverSnapshot.child("todaNumber").getValue(String::class.java) ?: ""

                        // Set payment mode
                        updates["bookings/$bookingId/paymentMode"] = paymentMode
                        updates["bookingIndex/$bookingId/paymentMode"] = paymentMode

                        // Set other driver details
                        if (driverRFID.isNotEmpty()) {
                            updates["bookings/$bookingId/driverRFID"] = driverRFID
                            updates["bookingIndex/$bookingId/driverRFID"] = driverRFID
                        }
                        if (driverName.isNotEmpty()) {
                            updates["bookings/$bookingId/driverName"] = driverName
                        }
                        if (todaNumber.isNotEmpty()) {
                            updates["bookings/$bookingId/assignedTricycleId"] = todaNumber
                            updates["bookings/$bookingId/todaNumber"] = todaNumber
                        }

                        println("✓ Driver assignment with payment mode: ID=$userId, PaymentMode=$paymentMode")
                    } else {
                        // Fallback: default payment mode
                        updates["bookings/$bookingId/paymentMode"] = "pay_every_trip"
                    }
                } catch (e: Exception) {
                    println("⚠ Error fetching driver details: ${e.message}")
                    updates["bookings/$bookingId/paymentMode"] = "pay_every_trip"
                }
            }

            // If completing the trip, record completionTime
            if (status == "COMPLETED") {
                updates["bookings/$bookingId/completionTime"] = System.currentTimeMillis()

                // Remove driver from queue when trip is COMPLETED
                // Get the driver RFID from the booking
                val bookingSnapshot = bookingsRef.child(bookingId).get().await()
                val driverRFID = bookingSnapshot.child("driverRFID").getValue(String::class.java) ?: ""

                if (driverRFID.isNotEmpty()) {
                    // Find and remove driver from queue
                    val queueRef = database.child("queue")
                    val queueSnapshot = queueRef.get().await()

                    for (child in queueSnapshot.children) {
                        val queueDriverRFID = child.child("driverRFID").getValue(String::class.java) ?: ""
                        if (queueDriverRFID == driverRFID) {
                            child.key?.let { queueKey ->
                                queueRef.child(queueKey).removeValue().await()
                                println("✓ Driver $driverRFID removed from queue after completing trip")
                            }
                            break
                        }
                    }
                }
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

                println("=== FETCHING DRIVER PAYMENT MODE ===")
                println("Driver ID: $userId")
                println("Booking ID: $bookingId")

                // Fetch the driver's details from the drivers table using the user ID
                try {
                    val driverSnapshot = hardwareDriversRef.child(userId).get().await()

                    if (driverSnapshot.exists()) {
                        println("✓ Driver found in drivers table")

                        // Get driver data from hardware drivers table
                        val driverRFID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""
                        val driverName = driverSnapshot.child("driverName").getValue(String::class.java) ?: ""
                        val todaNumber = driverSnapshot.child("todaNumber").getValue(String::class.java) ?: ""
                        val paymentMode = driverSnapshot.child("paymentMode").getValue(String::class.java) ?: "pay_every_trip"

                        println("Driver Data Retrieved:")
                        println("  - RFID: $driverRFID")
                        println("  - Name: $driverName")
                        println("  - TODA: $todaNumber")
                        println("  - Payment Mode: $paymentMode")

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

                        // ALWAYS set payment mode - even if empty, set the default
                        val finalPaymentMode = if (paymentMode.isNotEmpty()) paymentMode else "pay_every_trip"
                        updates["bookings/$bookingId/paymentMode"] = finalPaymentMode
                        updates["bookingIndex/$bookingId/paymentMode"] = finalPaymentMode

                        println("✓ Setting payment mode to: $finalPaymentMode")
                        println("✓ Driver assignment: ID=$userId, RFID=$driverRFID, Name=$driverName, TODA=$todaNumber, PaymentMode=$finalPaymentMode")
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
                        // Default payment mode for fallback
                        updates["bookings/$bookingId/paymentMode"] = "pay_every_trip"
                        updates["bookingIndex/$bookingId/paymentMode"] = "pay_every_trip"
                        println("✓ Using fallback - set payment mode to: pay_every_trip")
                    }
                } catch (e: Exception) {
                    println("⚠ Error fetching driver profile: ${e.message}")
                    e.printStackTrace()
                    // Fallback: set user ID as driverRFID
                    updates["bookings/$bookingId/driverRFID"] = userId
                    updates["bookingIndex/$bookingId/driverRFID"] = userId
                    // Default payment mode for fallback
                    updates["bookings/$bookingId/paymentMode"] = "pay_every_trip"
                    updates["bookingIndex/$bookingId/paymentMode"] = "pay_every_trip"
                    println("✓ Error fallback - set payment mode to: pay_every_trip")
                }
            }

            // Update bookingIndex status
            updates["bookingIndex/$bookingId/status"] = status

            println("=== FINAL UPDATE MAP ===")
            updates.forEach { (key, value) ->
                println("  $key: $value")
            }

            // Update the booking
            database.updateChildren(updates).await()

            println("✓ Database updated successfully")

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
    email: String,
    address: String,
    emergencyContact: String,
    licenseNumber: String,
    licenseExpiry: Long,
    yearsOfExperience: Int,
    tricyclePlateNumber: String,
    licensePhotoURL: String,
    selfiePhotoURL: String
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
                "email" to email,
                "address" to address,
                "emergencyContact" to emergencyContact,
                "licenseNumber" to licenseNumber,
                "licenseExpiry" to licenseExpiry,
                "yearsOfExperience" to yearsOfExperience,
                "tricyclePlateNumber" to tricyclePlateNumber,
                "licensePhotoURL" to licensePhotoURL,
                "selfiePhotoURL" to selfiePhotoURL,
                "rfidUID" to "",
                "isActive" to false,
                "registrationDate" to System.currentTimeMillis(),
                "status" to "PENDING_APPROVAL",
                "hasRfidAssigned" to false,
                "hasTodaMembershipAssigned" to false,
                // Initialize payment-related fields
                "balance" to 0.0,
                "paymentMode" to "",
                "canGoOnline" to true,
                "lastPaymentDate" to 0L
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

            // First get the driver's RFID - check both rfidNumber (new) and rfidUID (legacy)
            val driverSnapshot = hardwareDriversRef.child(driverId).get().await()
            val rfidNumber = driverSnapshot.child("rfidNumber").getValue(String::class.java) ?: ""
            val rfidUID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""

            // Prioritize rfidNumber if it exists, otherwise use rfidUID
            val driverRFID = if (rfidNumber.isNotEmpty()) rfidNumber else rfidUID

            println("Driver RFID (rfidNumber): $rfidNumber")
            println("Driver RFID (rfidUID): $rfidUID")
            println("Using RFID: $driverRFID")

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
                val paidFlag = child.child("paid").getValue(Boolean::class.java) ?: true

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
                        source = "mobile",
                        paid = paidFlag
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

            // First, find the driver by RFID - check both rfidNumber and rfidUID
            var driverSnapshot = hardwareDriversRef.orderByChild("rfidNumber").equalTo(rfidUID).get().await()

            if (!driverSnapshot.exists()) {
                println("No driver found with rfidNumber: $rfidUID, trying rfidUID...")
                driverSnapshot = hardwareDriversRef.orderByChild("rfidUID").equalTo(rfidUID).get().await()
            }

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

    // Real-time observer for driver RFID changes
    fun observeDriverRfid(driverId: String): Flow<String> = callbackFlow {
        println("=== STARTING REAL-TIME RFID OBSERVER ===")
        println("Observing RFID for driver ID: $driverId")

        if (driverId.isEmpty()) {
            println("Driver ID is empty, emitting empty string")
            trySend("").isSuccess
            close()
            return@callbackFlow
        }

        // Watch the entire driver node to detect changes in both rfidUID and rfidNumber
        val listener = hardwareDriversRef.child(driverId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Check rfidNumber first (newer field), then fall back to rfidUID (legacy)
                    val rfidNumber = snapshot.child("rfidNumber").getValue(String::class.java) ?: ""
                    val rfidUID = snapshot.child("rfidUID").getValue(String::class.java) ?: ""

                    // Prioritize rfidNumber if it exists, otherwise use rfidUID
                    val currentRfid = if (rfidNumber.isNotEmpty()) rfidNumber else rfidUID

                    println("=== RFID UPDATE ===")
                    println("Driver $driverId RFID changed to: $currentRfid")
                    println("  rfidNumber: $rfidNumber")
                    println("  rfidUID: $rfidUID")
                    trySend(currentRfid).isSuccess
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Error observing RFID for driver $driverId: ${error.message}")
                    trySend("").isSuccess
                    close(error.toException())
                }
            })

        awaitClose {
            println("Removing RFID observer for driver $driverId")
            hardwareDriversRef.child(driverId).removeEventListener(listener)
        }
    }

    // Queue Management
    fun observeDriverQueue(): Flow<List<QueueEntry>> = callbackFlow {
        println("=== STARTING REAL-TIME QUEUE LIST OBSERVER ===")

        val queueRef = database.child("queue")

        val listener = queueRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("=== QUEUE LIST DATA CHANGED ===")
                val queueEntries = mutableListOf<QueueEntry>()

                snapshot.children.forEach { child ->
                    try {
                        val driverId = child.child("driverId").getValue(String::class.java) ?: ""
                        val driverName = child.child("driverName").getValue(String::class.java) ?: ""
                        val driverRFID = when (val rfidValue = child.child("driverRFID").value) {
                            is String -> rfidValue
                            is Number -> rfidValue.toString()
                            else -> ""
                        }
                        val todaNumber = child.child("todaNumber").getValue(String::class.java) ?: ""
                        val timestamp = when (val tsValue = child.child("timestamp").value) {
                            is Number -> tsValue.toLong()
                            is String -> tsValue.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val status = child.child("status").getValue(String::class.java) ?: "waiting"

                        if (status == "waiting") {
                            queueEntries.add(
                                QueueEntry(
                                    driverRFID = driverRFID,
                                    driverName = driverName,
                                    timestamp = timestamp,
                                    position = queueEntries.size + 1
                                )
                            )
                        }
                    } catch (e: Exception) {
                        println("Error parsing queue entry: ${e.message}")
                    }
                }

                println("Queue entries: ${queueEntries.size}")
                trySend(queueEntries)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Queue observer cancelled: ${error.message}")
                trySend(emptyList())
            }
        })

        awaitClose {
            println("Removing queue observer")
            queueRef.removeEventListener(listener)
        }
    }

    suspend fun leaveQueue(driverRFID: String): Boolean {
        return try {
            println("=== LEAVING QUEUE ===")
            println("Driver RFID: $driverRFID")

            val queueRef = database.child("queue")
            val snapshot = queueRef.get().await()

            var removed = false
            snapshot.children.forEach { child ->
                val queueDriverRFID = when (val rfidValue = child.child("driverRFID").value) {
                    is String -> rfidValue
                    is Number -> rfidValue.toString()
                    else -> ""
                }

                if (queueDriverRFID == driverRFID) {
                    child.key?.let { key ->
                        queueRef.child(key).removeValue().await()
                        println("✓ Driver $driverRFID removed from queue")
                        removed = true
                    }
                    return@forEach
                }
            }

            if (!removed) {
                println("⚠ Driver $driverRFID not found in queue")
            }

            removed
        } catch (e: Exception) {
            println("✗ Error leaving queue: ${e.message}")
            false
        }
    }

    suspend fun joinQueue(driverId: String, driverRFID: String, driverName: String): Boolean {
        return try {
            println("=== JOINING QUEUE ===")
            println("Driver ID: $driverId, RFID: $driverRFID, Name: $driverName")

            // Get driver's TODA number
            val driverSnapshot = hardwareDriversRef.child(driverId).get().await()
            val todaNumber = driverSnapshot.child("todaNumber").getValue(String::class.java) ?: ""

            val queueRef = database.child("queue")
            val timestamp = System.currentTimeMillis()

            val queueEntry = mapOf(
                "driverId" to driverId,
                "driverName" to driverName,
                "driverRFID" to driverRFID,
                "todaNumber" to todaNumber,
                "timestamp" to timestamp,
                "status" to "waiting"
            )

            // Use timestamp as key for automatic sorting
            queueRef.child(timestamp.toString()).setValue(queueEntry).await()

            println("✓ Driver $driverName joined queue at position based on timestamp $timestamp")
            true
        } catch (e: Exception) {
            println("✗ Error joining queue: ${e.message}")
            false
        }
    }

    suspend fun getDriverTodayStats(driverId: String): Triple<Int, Double, Double> {
        return try {
            println("=== GETTING DRIVER TODAY STATS ===")
            println("Driver ID: $driverId")

            // Get today's start timestamp
            val todayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Get all bookings for this driver
            val bookingsSnapshot = bookingsRef.orderByChild("assignedDriverId").equalTo(driverId).get().await()

            var todayTrips = 0
            var todayEarnings = 0.0

            bookingsSnapshot.children.forEach { child ->
                val status = child.child("status").getValue(String::class.java) ?: ""
                val timestamp = when (val tsValue = child.child("timestamp").value) {
                    is Number -> tsValue.toLong()
                    is String -> tsValue.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val fare = when (val fareValue = child.child("estimatedFare").value) {
                    is Number -> fareValue.toDouble()
                    is String -> fareValue.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }

                // Count completed trips from today
                if (status == "COMPLETED" && timestamp >= todayStart) {
                    todayTrips++
                    todayEarnings += fare
                }
            }

            // Get driver's rating from ratings table
            var driverRating = 5.0
            var totalRatings = 0
            var sumRatings = 0.0

            val ratingsSnapshot = ratingsRef.orderByChild("driverId").equalTo(driverId).get().await()
            ratingsSnapshot.children.forEach { child ->
                val stars = when (val starsValue = child.child("stars").value) {
                    is Number -> starsValue.toInt()
                    is String -> starsValue.toIntOrNull() ?: 0
                    else -> 0
                }

                if (stars > 0) {
                    totalRatings++
                    sumRatings += stars.toDouble()
                }
            }

            if (totalRatings > 0) {
                driverRating = sumRatings / totalRatings
            }

            println("Driver stats: $todayTrips trips, ₱$todayEarnings earnings, $driverRating rating")
            Triple(todayTrips, todayEarnings, driverRating)
        } catch (e: Exception) {
            println("✗ Error getting driver stats: ${e.message}")
            Triple(0, 0.0, 5.0)
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

    // Additional booking management functions
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

                println("\n=== CHECKING QUEUE ENTRY: $queueKey ===")

                // Check if already claimed
                val alreadyClaimed = queueEntry.child("claimed").getValue(Boolean::class.java) ?: false
                val queueStatus = queueEntry.child("status").getValue(String::class.java) ?: "waiting"

                println("Claimed: $alreadyClaimed, Status: $queueStatus")

                if (alreadyClaimed && queueStatus != "waiting") {
                    println("✗ Skipping - already claimed")
                    continue
                }

                // Get driver details
                val driverRFID = queueEntry.child("driverRFID").getValue(String::class.java) ?: ""
                val driverName = queueEntry.child("driverName").getValue(String::class.java) ?: ""
                val todaNumber = queueEntry.child("todaNumber").getValue(String::class.java) ?: ""

                println("Driver RFID: $driverRFID, Name: $driverName, TODA: $todaNumber")

                if (driverRFID.isBlank()) {
                    println("✗ Skipping - blank RFID")
                    continue
                }

                // Look up driver user ID
                var driverUserId = ""
                try {
                    val driversSnapshot = hardwareDriversRef.orderByChild("rfidUID").equalTo(driverRFID).get().await()
                    if (driversSnapshot.exists()) {
                        driverUserId = driversSnapshot.children.firstOrNull()?.key ?: ""
                        println("✓ Found driver user ID: $driverUserId")
                    } else {
                        println("⚠ No driver found with RFID in drivers table")
                    }
                } catch (e: Exception) {
                    println("Error looking up driver: ${e.message}")
                }

                if (driverUserId.isEmpty()) {
                    driverUserId = driverRFID
                    println("Using RFID as driver ID (fallback)")
                }

                println("\n=== FETCHING PAYMENT MODE (AUTO-MATCH) ===")
                println("Driver ID: $driverUserId")

                // Get driver's payment mode
                var paymentMode = "pay_every_trip"
                try {
                    val driverSnapshot = hardwareDriversRef.child(driverUserId).get().await()
                    if (driverSnapshot.exists()) {
                        val fetchedMode = driverSnapshot.child("paymentMode").getValue(String::class.java)
                        println("✓ Driver found - Payment Mode in DB: '$fetchedMode'")
                        paymentMode = if (!fetchedMode.isNullOrBlank()) fetchedMode else "pay_every_trip"
                        println("✓ Final Payment Mode: '$paymentMode'")
                    } else {
                        println("⚠ Driver not found, using default: pay_every_trip")
                    }
                } catch (e: Exception) {
                    println("Error fetching driver payment mode: ${e.message}")
                }

                // Update booking
                val updates = mutableMapOf<String, Any>(
                    "bookings/$bookingId/assignedDriverId" to driverUserId,
                    "bookings/$bookingId/driverRFID" to driverRFID,
                    "bookings/$bookingId/driverName" to driverName,
                    "bookings/$bookingId/assignedTricycleId" to todaNumber,
                    "bookings/$bookingId/todaNumber" to todaNumber,
                    "bookings/$bookingId/paymentMode" to paymentMode,
                    "bookings/$bookingId/status" to "ACCEPTED",
                    "bookingIndex/$bookingId/status" to "ACCEPTED",
                    "bookingIndex/$bookingId/driverRFID" to driverRFID,
                    "bookingIndex/$bookingId/paymentMode" to paymentMode
                )

                database.updateChildren(updates).await()
                queueRef.child(queueKey).removeValue().await()

                println("✅ Matched booking $bookingId with driver $driverUserId (PaymentMode: $paymentMode)")
                return true
            }

            false
        } catch (e: Exception) {
            println("Error matching booking: ${e.message}")
            false
        }
    }

    suspend fun tryMatchBookingToFirstDriver(bookingId: String): Boolean {
        return matchBookingToFirstDriver(bookingId)
    }

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

    suspend fun createRatingEntry(bookingId: String): Boolean {
        return try {
            println("=== CREATING RATING ENTRY ===")
            println("Booking ID: $bookingId")

            val bookingSnapshot = bookingsRef.child(bookingId).get().await()
            val booking = bookingSnapshot.getValue(FirebaseBooking::class.java)

            if (booking == null) {
                println("✗ Booking not found: $bookingId")
                return false
            }

            val ratingRef = ratingsRef.push()
            val ratingId = ratingRef.key ?: return false

            val rating = FirebaseRating(
                id = ratingId,
                bookingId = bookingId,
                customerId = booking.customerId,
                customerName = booking.customerName,
                driverId = booking.assignedDriverId,
                driverName = booking.driverName,
                stars = 0,
                feedback = "",
                timestamp = System.currentTimeMillis(),
                ratedBy = "DRIVER"
            )

            ratingRef.setValue(rating).await()
            println("✓ Rating entry created: $ratingId for booking $bookingId")
            true
        } catch (e: Exception) {
            println("✗ Error creating rating entry: ${e.message}")
            false
        }
    }

    suspend fun updateRating(bookingId: String, stars: Int, feedback: String): Boolean {
        return try {
            val ratingSnapshot = ratingsRef.orderByChild("bookingId").equalTo(bookingId).get().await()

            if (!ratingSnapshot.exists()) {
                println("✗ No rating entry found for booking: $bookingId")
                return false
            }

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
                false
            }
        } catch (e: Exception) {
            println("✗ Error updating rating: ${e.message}")
            false
        }
    }

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

    fun observeDriverBalance(driverId: String): Flow<Double> = callbackFlow {
        println("=== STARTING REAL-TIME BALANCE OBSERVER ===")
        println("Observing balance for driver ID: $driverId")

        if (driverId.isEmpty()) {
            println("Driver ID is empty, emitting 0.0")
            trySend(0.0).isSuccess
            close()
            return@callbackFlow
        }

        val driverRef = hardwareDriversRef.child(driverId)

        val listener = driverRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("=== BALANCE DATA CHANGED ===")
                val balance = snapshot.child("balance").getValue(Double::class.java) ?: 0.0
                println("Driver $driverId balance: ₱$balance")

                val result = trySend(balance)
                if (result.isFailure) {
                    println("ERROR: Failed to emit balance: ${result.exceptionOrNull()}")
                } else {
                    println("SUCCESS: Emitted balance: ₱$balance")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Balance observer cancelled: ${error.message}")
                trySend(0.0).isSuccess
                close(error.toException())
            }
        })

        awaitClose {
            println("Removing balance observer for driver $driverId")
            driverRef.removeEventListener(listener)
        }
    }

    fun observeDriverPaymentMode(driverId: String): Flow<String?> = callbackFlow {
        println("=== STARTING REAL-TIME PAYMENT MODE OBSERVER ===")
        println("Observing payment mode for driver ID: $driverId")

        if (driverId.isEmpty()) {
            println("Driver ID is empty, emitting null")
            trySend(null).isSuccess
            close()
            return@callbackFlow
        }

        val driverRef = hardwareDriversRef.child(driverId)

        val listener = driverRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("=== PAYMENT MODE DATA CHANGED ===")
                val paymentMode = snapshot.child("paymentMode").getValue(String::class.java)
                println("Driver $driverId payment mode: $paymentMode")

                val result = trySend(paymentMode)
                if (result.isFailure) {
                    println("ERROR: Failed to emit payment mode: ${result.exceptionOrNull()}")
                } else {
                    println("SUCCESS: Emitted payment mode: $paymentMode")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Payment mode observer cancelled: ${error.message}")
                trySend(null).isSuccess
                close(error.toException())
            }
        })

        awaitClose {
            println("Removing payment mode observer for driver $driverId")
            driverRef.removeEventListener(listener)
        }
    }

    fun observePayBalanceStatus(driverId: String): Flow<Boolean> = callbackFlow {
        println("=== STARTING REAL-TIME PAY_BALANCE OBSERVER ===")
        println("Observing pay_balance for driver ID: $driverId")

        if (driverId.isEmpty()) {
            println("Driver ID is empty, emitting false")
            trySend(false).isSuccess
            close()
            return@callbackFlow
        }

        val driverRef = hardwareDriversRef.child(driverId)

        val listener = driverRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("=== PAY_BALANCE DATA CHANGED ===")
                val payBalance = snapshot.child("pay_balance").getValue(Boolean::class.java) ?: false
                println("Driver $driverId pay_balance: $payBalance")

                val result = trySend(payBalance)
                if (result.isFailure) {
                    println("ERROR: Failed to emit pay_balance: ${result.exceptionOrNull()}")
                } else {
                    println("SUCCESS: Emitted pay_balance: $payBalance")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Pay_balance observer cancelled: ${error.message}")
                trySend(false).isSuccess
                close(error.toException())
            }
        })

        awaitClose {
            println("Removing pay_balance observer for driver $driverId")
            driverRef.removeEventListener(listener)
        }
    }

    suspend fun markPayBalance(driverId: String, wantsToPay: Boolean): Boolean {
        return try {
            println("=== MARKING PAY_BALANCE ===")
            println("Driver ID: $driverId, Wants to Pay: $wantsToPay")

            val updates = mapOf(
                "drivers/$driverId/pay_balance" to wantsToPay,
                "drivers/$driverId/pay_balance_timestamp" to System.currentTimeMillis()
            )

            database.updateChildren(updates).await()
            println("✓ Successfully updated pay_balance to $wantsToPay for driver $driverId")
            true
        } catch (e: Exception) {
            println("✗ Error updating pay_balance: ${e.message}")
            false
        }
    }

    fun getRfidChangeHistory(driverId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        println("=== GETTING RFID CHANGE HISTORY ===")
        println("Driver ID: $driverId")
        println("Querying from: rfidHistory/$driverId")

        // Query the nested structure: rfidHistory/{driverId}/{historyId}
        val driverHistoryRef = rfidChangeHistoryRef.child(driverId)

        val listener = driverHistoryRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("=== RFID HISTORY DATA RECEIVED ===")
                    println("Snapshot exists: ${snapshot.exists()}")
                    println("Number of history entries: ${snapshot.childrenCount}")

                    val history = mutableListOf<Map<String, Any>>()
                    snapshot.children.forEach { child ->
                        println("Processing history entry: ${child.key}")
                        @Suppress("UNCHECKED_CAST")
                        val historyMap = child.value as? Map<String, Any>
                        if (historyMap != null) {
                            // Add the ID from the key if not present
                            val enrichedMap = historyMap.toMutableMap()
                            if (!enrichedMap.containsKey("id")) {
                                enrichedMap["id"] = child.key ?: ""
                            }
                            // Ensure driverId is present
                            if (!enrichedMap.containsKey("driverId")) {
                                enrichedMap["driverId"] = driverId
                            }
                            history.add(enrichedMap)
                            println("  Added history entry: $enrichedMap")
                        } else {
                            println("  Skipped null history entry")
                        }
                    }

                    // Sort by timestamp descending (newest first)
                    val sortedHistory = history.sortedByDescending {
                        (it["reassignedAt"] as? String)?.let { dateStr ->
                            try {
                                // Try to parse ISO date string
                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).parse(dateStr)?.time
                            } catch (e: Exception) {
                                // Fallback to timestamp field if exists
                                (it["timestamp"] as? Number)?.toLong() ?: 0L
                            }
                        } ?: (it["timestamp"] as? Number)?.toLong() ?: 0L
                    }

                    println("Total history entries found: ${sortedHistory.size}")
                    trySend(sortedHistory)
                }
                override fun onCancelled(error: DatabaseError) {
                    println("=== RFID HISTORY QUERY CANCELLED ===")
                    println("Error: ${error.message}")
                    trySend(emptyList())
                }
            })
        awaitClose {
            println("Removing RFID history listener for driver $driverId")
            driverHistoryRef.removeEventListener(listener)
        }
    }

    suspend fun reportMissingRfid(driverId: String): Boolean {
        return try {
            println("=== REPORTING MISSING RFID ===")
            println("Driver ID: $driverId")

            val updates = mapOf(
                "drivers/$driverId/rfidMissing" to true,
                "drivers/$driverId/rfidMissingReportedAt" to System.currentTimeMillis()
            )

            database.updateChildren(updates).await()
            println("✓ Successfully marked RFID as missing for driver $driverId")
            true
        } catch (e: Exception) {
            println("✗ Error reporting missing RFID: ${e.message}")
            false
        }
    }

    // Fix existing booking by adding payment mode from driver's current payment mode
    suspend fun fixBookingPaymentMode(bookingId: String): Boolean {
        return try {
            println("=== FIXING BOOKING PAYMENT MODE ===")
            println("Booking ID: $bookingId")

            // Get the booking to find the assigned driver
            val bookingSnapshot = bookingsRef.child(bookingId).get().await()
            if (!bookingSnapshot.exists()) {
                println("✗ Booking not found: $bookingId")
                return false
            }

            val assignedDriverId = bookingSnapshot.child("assignedDriverId").getValue(String::class.java)
            if (assignedDriverId.isNullOrEmpty()) {
                println("✗ No driver assigned to booking: $bookingId")
                return false
            }

            println("✓ Found assigned driver: $assignedDriverId")

            // Get the driver's payment mode
            val driverSnapshot = hardwareDriversRef.child(assignedDriverId).get().await()
            if (!driverSnapshot.exists()) {
                println("✗ Driver not found: $assignedDriverId")
                return false
            }

            val paymentMode = driverSnapshot.child("paymentMode").getValue(String::class.java) ?: "pay_every_trip"
            println("✓ Driver payment mode: $paymentMode")

            // Update the booking with payment mode
            val updates = mapOf(
                "bookings/$bookingId/paymentMode" to paymentMode,
                "bookingIndex/$bookingId/paymentMode" to paymentMode
            )

            database.updateChildren(updates).await()
            println("✅ Successfully added payment mode '$paymentMode' to booking $bookingId")
            true
        } catch (e: Exception) {
            println("✗ Error fixing booking payment mode: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Fix all bookings for a specific driver that are missing payment mode
    suspend fun fixAllDriverBookingsPaymentMode(driverId: String): Int {
        return try {
            println("=== FIXING ALL BOOKINGS FOR DRIVER ===")
            println("Driver ID: $driverId")

            // Get driver's payment mode
            val driverSnapshot = hardwareDriversRef.child(driverId).get().await()
            if (!driverSnapshot.exists()) {
                println("✗ Driver not found: $driverId")
                return 0
            }

            val paymentMode = driverSnapshot.child("paymentMode").getValue(String::class.java) ?: "pay_every_trip"
            println("✓ Driver payment mode: $paymentMode")

            // Get all bookings for this driver
            val bookingsSnapshot = bookingsRef.orderByChild("assignedDriverId").equalTo(driverId).get().await()

            var fixedCount = 0
            val updates = mutableMapOf<String, Any>()

            bookingsSnapshot.children.forEach { child ->
                val bookingId = child.key ?: return@forEach
                val existingPaymentMode = child.child("paymentMode").getValue(String::class.java)

                // Only update if payment mode is missing or empty
                if (existingPaymentMode.isNullOrEmpty()) {
                    updates["bookings/$bookingId/paymentMode"] = paymentMode
                    updates["bookingIndex/$bookingId/paymentMode"] = paymentMode
                    fixedCount++
                    println("  Adding payment mode to booking: $bookingId")
                }
            }

            if (updates.isNotEmpty()) {
                database.updateChildren(updates).await()
                println("✅ Successfully fixed $fixedCount bookings")
            } else {
                println("✓ No bookings needed fixing - all have payment mode set")
            }

            fixedCount
        } catch (e: Exception) {
            println("✗ Error fixing driver bookings: ${e.message}")
            e.printStackTrace()
            0
        }
    }

}
