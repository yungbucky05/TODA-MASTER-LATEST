package com.example.toda.repository

import com.example.toda.data.*
import com.example.toda.service.FirebaseRealtimeDatabaseService
import com.example.toda.service.FirebaseAuthService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.combine

@Singleton
class TODARepository @Inject constructor(
    private val firebaseService: FirebaseRealtimeDatabaseService,
    private val authService: FirebaseAuthService
) {

    // User Management
    suspend fun registerUser(
        phoneNumber: String,
        name: String,
        userType: UserType,
        password: String
    ): Result<String> {
        return try {
            // First create user in Firebase Auth
            val authResult = authService.createUserWithPhoneNumber(phoneNumber, password)

            authResult.fold(
                onSuccess = { userId ->
                    // Then create user profile in Realtime Database
                    val user = FirebaseUser(
                        id = userId,
                        phoneNumber = phoneNumber,
                        name = name,
                        userType = userType.name,
                        isVerified = true, // Since we're using Firebase Auth
                        registrationDate = System.currentTimeMillis()
                    )

                    val success = firebaseService.createUser(user)
                    if (success) {
                        Result.success(userId)
                    } else {
                        Result.failure(Exception("Failed to create user profile"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Overloaded function for customer registration with extended user data
    suspend fun registerUser(
        phoneNumber: String,
        password: String,
        userData: Map<String, Any>
    ): Result<FirebaseUser> {
        return try {
            // First create user in Firebase Auth
            val authResult = authService.createUserWithPhoneNumber(phoneNumber, password)

            authResult.fold(
                onSuccess = { userId ->
                    // Create a user object with the provided user data
                    val user = FirebaseUser(
                        id = userId,
                        phoneNumber = phoneNumber,
                        name = userData["name"] as? String ?: "",
                        userType = userData["userType"] as? String ?: "PASSENGER",
                        isVerified = userData["verified"] as? Boolean ?: true,
                        registrationDate = userData["registrationDate"] as? Long ?: System.currentTimeMillis()
                    )

                    // Add the userId to userData
                    val updatedUserData = userData + ("id" to userId)

                    // Then create user profile in Realtime Database
                    val success = firebaseService.createUserWithData(userId, updatedUserData)
                    if (success) {
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Failed to create user profile"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Overloaded function for customer registration with email and extended user data
    suspend fun registerUser(
        email: String,
        phoneNumber: String,
        password: String,
        userData: Map<String, Any>
    ): Result<FirebaseUser> {
        return try {
            // First create user in Firebase Auth with email
            val authResult = authService.createUserWithEmail(email, password)

            authResult.fold(
                onSuccess = { userId ->
                    // Create a user object with the provided user data
                    val user = FirebaseUser(
                        id = userId,
                        phoneNumber = phoneNumber,
                        name = userData["name"] as? String ?: "",
                        userType = userData["userType"] as? String ?: "PASSENGER",
                        isVerified = userData["verified"] as? Boolean ?: true,
                        registrationDate = userData["registrationDate"] as? Long ?: System.currentTimeMillis()
                    )

                    // Add the userId to userData
                    val updatedUserData = userData + ("id" to userId)

                    // Then create user profile in Realtime Database
                    val success = firebaseService.createUserWithData(userId, updatedUserData)
                    if (success) {
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Failed to create user profile"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByPhoneNumber(phoneNumber: String): FirebaseUser? {
        return try {
            firebaseService.getUserByPhoneNumber(phoneNumber)
        } catch (e: Exception) {
            println("Error getting user by phone number: ${e.message}")
            null
        }
    }

    suspend fun loginUser(phoneNumber: String, password: String): Result<FirebaseUser> {
        return try {
            println("=== CUSTOMER LOGIN DEBUG ===")
            println("Attempting login for phone: $phoneNumber")

            // First authenticate with Firebase Auth
            val authResult = authService.signInWithPhoneNumber(phoneNumber, password)

            authResult.fold(
                onSuccess = { userId ->
                    println("Firebase Auth login successful for userId: $userId")

                    // Then get the user profile from Realtime Database
                    val userProfile = firebaseService.getUserByPhoneNumber(phoneNumber)

                    if (userProfile != null) {
                        println("User profile found - Type: ${userProfile.userType}, Verified: ${userProfile.isVerified}")

                        // FETCH EXTENDED PROFILE DATA IMMEDIATELY
                        println("Fetching extended profile data for user: $userId")
                        try {
                            val extendedProfile = firebaseService.getUserProfile(userId).first()
                            if (extendedProfile != null) {
                                println("Extended profile loaded - Discount Type: ${extendedProfile.discountType}, Total Bookings: ${extendedProfile.totalBookings}")
                            } else {
                                println("No extended profile found - will use basic profile only")
                            }
                        } catch (e: Exception) {
                            println("Error fetching extended profile during login: ${e.message}")
                        }

                        Result.success(userProfile)
                    } else {
                        println("User authenticated but no profile found. Creating profile...")

                        // If auth is successful but no profile exists, create one
                        // This handles cases where the profile creation might have failed during registration
                        val newUser = FirebaseUser(
                            id = userId,
                            phoneNumber = phoneNumber,
                            name = "Customer", // Default name, should be updated in profile
                            userType = "PASSENGER",
                            isVerified = true,
                            registrationDate = System.currentTimeMillis()
                        )

                        val profileCreated = firebaseService.createUser(newUser)
                        if (profileCreated) {
                            println("Profile created successfully")
                            Result.success(newUser)
                        } else {
                            Result.failure(Exception("Authentication successful but failed to create user profile"))
                        }
                    }
                },
                onFailure = { error ->
                    println("Firebase Auth login failed: ${error.message}")

                    // IMPORTANT: Do NOT allow login on auth failure.
                    // Instead, provide a helpful error. If the account exists, hint to reset password.
                    return@fold try {
                        val existingProfile = firebaseService.getUserByPhoneNumber(phoneNumber)
                        if (existingProfile != null) {
                            Result.failure(Exception("Account exists but the password is incorrect. Please reset your password."))
                        } else {
                            Result.failure(Exception("Invalid credentials. Please check your phone number and password."))
                        }
                    } catch (e: Exception) {
                        Result.failure(Exception("Authentication failed: ${error.message ?: "Unknown error"}"))
                    }
                }
            )
        } catch (e: Exception) {
            println("Login error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, profile: UserProfile): Result<Unit> {
        return try {
            val firebaseProfile = FirebaseUserProfile(
                phoneNumber = profile.phoneNumber,
                name = profile.name,
                userType = profile.userType.name,
                address = profile.address,
                emergencyContact = profile.emergencyContact,
                profilePicture = profile.profilePicture,
                totalBookings = profile.totalBookings,
                completedBookings = profile.completedBookings,
                cancelledBookings = profile.cancelledBookings,
                trustScore = profile.trustScore,
                isBlocked = profile.isBlocked,
                lastBookingTime = profile.lastBookingTime,
                discountType = profile.discountType?.name,
                discountIdNumber = profile.discountIdNumber,
                discountIdImageUrl = profile.discountIdImageUrl,
                discountVerified = profile.discountVerified,
                discountExpiryDate = profile.discountExpiryDate,
                licenseNumber = profile.licenseNumber,
                licenseExpiry = profile.licenseExpiry,
                yearsOfExperience = profile.yearsOfExperience,
                rating = profile.rating,
                totalTrips = profile.totalTrips,
                earnings = profile.earnings
            )

            val success = firebaseService.updateUserProfile(userId, firebaseProfile)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserProfile(userId: String): Flow<UserProfile?> {
        // Combine profile stored under 'userProfiles' with raw user data under 'users'
        val profileFlow = firebaseService.getUserProfile(userId)
        val userRawFlow = firebaseService.getUserRaw(userId)

        return combine(profileFlow, userRawFlow) { firebaseProfile, userRaw ->
            // Helper to parse discountType from raw map which could be String or Map (enum object)
            fun parseDiscountType(raw: Any?): DiscountType? {
                return when (raw) {
                    is String -> try {
                        DiscountType.valueOf(raw)
                    } catch (_: IllegalArgumentException) { null }
                    is Map<*, *> -> {
                        // Try to map by displayName
                        val displayName = raw["displayName"] as? String
                        when (displayName) {
                            "Person with Disability" -> DiscountType.PWD
                            "Senior Citizen" -> DiscountType.SENIOR_CITIZEN
                            "Student" -> DiscountType.STUDENT
                            else -> null
                        }
                    }
                    else -> null
                }
            }

            // Fields possibly stored in 'users'
            val rawDiscountType: DiscountType? = parseDiscountType(userRaw?.get("discountType"))
            val rawDiscountVerified: Boolean? = (userRaw?.get("discountVerified") as? Boolean)
            val rawDiscountExpiry: Long? = when (val v = userRaw?.get("discountExpiryDate")) {
                is Number -> v.toLong()
                is String -> v.toLongOrNull()
                else -> null
            }
            val rawDiscountIdNumber: String? = userRaw?.get("discountIdNumber") as? String
            val rawDiscountIdImageUrl: String? = userRaw?.get("discountIdImageUrl") as? String

            // Build unified UserProfile, preferring firebaseProfile for general fields,
            // and overlaying discount fields from 'users' when present
            firebaseProfile?.let { profile ->
                UserProfile(
                    phoneNumber = profile.phoneNumber,
                    name = profile.name,
                    userType = try { UserType.valueOf(profile.userType) } catch (_: Exception) { UserType.PASSENGER },
                    address = profile.address,
                    emergencyContact = profile.emergencyContact,
                    profilePicture = profile.profilePicture,
                    totalBookings = profile.totalBookings,
                    completedBookings = profile.completedBookings,
                    cancelledBookings = profile.cancelledBookings,
                    trustScore = profile.trustScore,
                    isBlocked = profile.isBlocked,
                    lastBookingTime = profile.lastBookingTime,
                    // Overlay discount fields from raw when available, else from profile
                    discountType = rawDiscountType ?: profile.discountType?.let { dt ->
                        try { DiscountType.valueOf(dt) } catch (_: Exception) { null }
                    },
                    discountIdNumber = rawDiscountIdNumber ?: profile.discountIdNumber,
                    discountIdImageUrl = rawDiscountIdImageUrl ?: profile.discountIdImageUrl,
                    discountVerified = rawDiscountVerified ?: profile.discountVerified,
                    discountExpiryDate = rawDiscountExpiry ?: profile.discountExpiryDate,
                    licenseNumber = profile.licenseNumber,
                    licenseExpiry = profile.licenseExpiry,
                    yearsOfExperience = profile.yearsOfExperience,
                    rating = profile.rating,
                    totalTrips = profile.totalTrips,
                    earnings = profile.earnings
                )
            } ?: run {
                // If no firebaseProfile exists, construct minimal profile from raw user data
                if (userRaw != null) {
                    UserProfile(
                        phoneNumber = userRaw["phoneNumber"] as? String ?: "",
                        name = userRaw["name"] as? String ?: "",
                        userType = try { UserType.valueOf((userRaw["userType"] as? String) ?: "PASSENGER") } catch (_: Exception) { UserType.PASSENGER },
                        address = userRaw["address"] as? String ?: "",
                        emergencyContact = userRaw["emergencyContact"] as? String ?: "",
                        totalBookings = (userRaw["totalBookings"] as? Number)?.toInt() ?: 0,
                        completedBookings = (userRaw["completedBookings"] as? Number)?.toInt() ?: 0,
                        cancelledBookings = (userRaw["cancelledBookings"] as? Number)?.toInt() ?: 0,
                        trustScore = (userRaw["trustScore"] as? Number)?.toDouble() ?: 100.0,
                        isBlocked = userRaw["isBlocked"] as? Boolean ?: false,
                        lastBookingTime = (userRaw["lastBookingTime"] as? Number)?.toLong() ?: 0L,
                        discountType = rawDiscountType,
                        discountIdNumber = rawDiscountIdNumber ?: "",
                        discountIdImageUrl = rawDiscountIdImageUrl ?: "",
                        discountVerified = rawDiscountVerified ?: false,
                        discountExpiryDate = rawDiscountExpiry,
                        licenseNumber = userRaw["licenseNumber"] as? String,
                        licenseExpiry = (userRaw["licenseExpiry"] as? Number)?.toLong(),
                        yearsOfExperience = (userRaw["yearsOfExperience"] as? Number)?.toInt() ?: 0,
                        rating = (userRaw["rating"] as? Number)?.toDouble() ?: 5.0,
                        totalTrips = (userRaw["totalTrips"] as? Number)?.toInt() ?: 0,
                        earnings = (userRaw["earnings"] as? Number)?.toDouble() ?: 0.0
                    )
                } else {
                    null
                }
            }
        }
    }

    // Booking Management
    suspend fun createBooking(booking: Booking): Result<String> {
        return try {
            println("=== REPOSITORY CREATE BOOKING DEBUG ===")
            println("Input booking: $booking")

            val firebaseBooking = FirebaseBooking(
                customerId = booking.customerId,
                customerName = booking.customerName,
                phoneNumber = booking.phoneNumber,
                isPhoneVerified = booking.isPhoneVerified,
                pickupLocation = booking.pickupLocation,
                destination = booking.destination,
                pickupCoordinates = mapOf(
                    "lat" to booking.pickupGeoPoint.latitude,
                    "lng" to booking.pickupGeoPoint.longitude
                ),
                dropoffCoordinates = mapOf(
                    "lat" to booking.dropoffGeoPoint.latitude,
                    "lng" to booking.dropoffGeoPoint.longitude
                ),
                estimatedFare = booking.estimatedFare,
                actualFare = booking.estimatedFare, // Initially same as estimated, will be updated later
                distance = calculateDistance(booking.pickupGeoPoint, booking.dropoffGeoPoint),
                status = booking.status.name,
                timestamp = booking.timestamp,
                assignedDriverId = "", // Initially empty
                assignedTricycleId = booking.assignedTricycleId ?: "",
                verificationCode = booking.verificationCode,
                paymentMethod = "CASH", // Default payment method
                duration = 0, // Will be calculated during trip
                // New key to indicate origin of booking
                tripType = "App Booking"
            )

            println("Converted Firebase booking: $firebaseBooking")
            println("Calling firebaseService.createBooking...")

            val bookingId = firebaseService.createBooking(firebaseBooking)

            println("Firebase service returned booking ID: $bookingId")

            if (bookingId != null) {
                // Immediately try to match with the first driver in the queue
                try {
                    val matched = firebaseService.matchBookingToFirstDriver(bookingId)
                    if (matched) {
                        println("✓ Booking $bookingId matched to first driver in queue")
                    } else {
                        println("No available driver in queue to match for booking $bookingId; remains PENDING")
                    }
                } catch (e: Exception) {
                    println("Error attempting to match booking to driver: ${e.message}")
                }

                println("SUCCESS: Repository createBooking completed with ID: $bookingId")
                Result.success(bookingId)
            } else {
                println("ERROR: Firebase service returned null booking ID")
                Result.failure(Exception("Failed to create booking"))
            }
        } catch (e: Exception) {
            println("ERROR: Exception in repository createBooking: ${e.message}")
            println("Exception type: ${e::class.java.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Helper function to calculate distance between two points
    private fun calculateDistance(pickup: org.osmdroid.util.GeoPoint, dropoff: org.osmdroid.util.GeoPoint): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(pickup.latitude)
        val lat2Rad = Math.toRadians(dropoff.latitude)
        val deltaLatRad = Math.toRadians(dropoff.latitude - pickup.latitude)
        val deltaLngRad = Math.toRadians(dropoff.longitude - pickup.longitude)

        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c // Distance in kilometers
    }

    fun getActiveBookings(): Flow<List<Booking>> {
        return firebaseService.getActiveBookings().map { firebaseBookings ->
            println("=== REPOSITORY ACTIVE BOOKINGS DEBUG ===")
            println("Firebase bookings count: ${firebaseBookings.size}")

            firebaseBookings.mapNotNull { firebaseBooking ->
                try {
                    println("Processing Firebase booking: ${firebaseBooking.id} with status: ${firebaseBooking.status}")

                    val booking = Booking(
                        id = firebaseBooking.id,
                        customerId = firebaseBooking.customerId,
                        customerName = firebaseBooking.customerName,
                        phoneNumber = firebaseBooking.phoneNumber,
                        isPhoneVerified = firebaseBooking.isPhoneVerified,
                        pickupLocation = firebaseBooking.pickupLocation,
                        destination = firebaseBooking.destination,
                        pickupGeoPoint = GeoPoint(
                            firebaseBooking.pickupCoordinates["lat"] ?: 0.0,
                            firebaseBooking.pickupCoordinates["lng"] ?: 0.0
                        ),
                        dropoffGeoPoint = GeoPoint(
                            firebaseBooking.dropoffCoordinates["lat"] ?: 0.0,
                            firebaseBooking.dropoffCoordinates["lng"] ?: 0.0
                        ),
                        estimatedFare = firebaseBooking.estimatedFare,
                        status = try {
                            BookingStatus.valueOf(firebaseBooking.status)
                        } catch (e: IllegalArgumentException) {
                            println("ERROR: Unknown booking status: ${firebaseBooking.status}, defaulting to PENDING")
                            BookingStatus.PENDING
                        },
                        timestamp = firebaseBooking.timestamp,
                        assignedTricycleId = firebaseBooking.assignedTricycleId,
                        verificationCode = firebaseBooking.verificationCode,
                        // Add the missing driver fields from FirebaseBooking
                        driverName = firebaseBooking.driverName,
                        driverRFID = firebaseBooking.driverRFID,
                        todaNumber = firebaseBooking.todaNumber,
                        assignedDriverId = firebaseBooking.assignedDriverId,
                        // Add arrival and no-show tracking fields
                        arrivedAtPickup = firebaseBooking.arrivedAtPickup,
                        arrivedAtPickupTime = firebaseBooking.arrivedAtPickupTime,
                        isNoShow = firebaseBooking.isNoShow,
                        noShowReportedTime = firebaseBooking.noShowReportedTime
                    )

                    println("Successfully converted booking: ${booking.id} with status: ${booking.status}")
                    booking
                } catch (e: Exception) {
                    println("ERROR: Failed to convert Firebase booking ${firebaseBooking.id}: ${e.message}")
                    null
                }
            }
        }
    }

    suspend fun acceptBooking(bookingId: String, driverId: String): Result<Unit> {
        return try {
            // Use the enhanced method that auto-creates chat rooms
            val success = firebaseService.updateBookingStatusWithChatRoom(bookingId, "ACCEPTED", driverId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to accept booking"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBookingStatusOnly(bookingId: String, status: String): Result<Unit> {
        return try {
            val success = firebaseService.updateBookingStatus(bookingId, status, null)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update booking status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeBooking(bookingId: String): Result<Unit> {
        return try {
            val success = firebaseService.updateBookingStatus(bookingId, "COMPLETED")
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to complete booking"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRatingEntry(bookingId: String): Result<Unit> {
        return try {
            val success = firebaseService.createRatingEntry(bookingId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create rating entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRating(bookingId: String, stars: Int, feedback: String): Result<Unit> {
        return try {
            val success = firebaseService.updateRating(bookingId, stars, feedback)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update rating"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark driver as arrived at pickup point
    suspend fun markArrivedAtPickup(bookingId: String): Result<Unit> {
        return try {
            val success = firebaseService.markArrivedAtPickup(bookingId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark arrived at pickup"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Report customer no-show
    suspend fun reportNoShow(bookingId: String): Result<Unit> {
        return try {
            val success = firebaseService.reportNoShow(bookingId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to report no-show"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get booking by ID once (non-streaming)
    suspend fun getBookingByIdOnce(bookingId: String): Booking? {
        return try {
            val firebaseBooking = firebaseService.getBookingById(bookingId) ?: return null
            Booking(
                id = firebaseBooking.id,
                customerId = firebaseBooking.customerId,
                customerName = firebaseBooking.customerName,
                phoneNumber = firebaseBooking.phoneNumber,
                isPhoneVerified = firebaseBooking.isPhoneVerified,
                pickupLocation = firebaseBooking.pickupLocation,
                destination = firebaseBooking.destination,
                pickupGeoPoint = GeoPoint(
                    firebaseBooking.pickupCoordinates["lat"] ?: 0.0,
                    firebaseBooking.pickupCoordinates["lng"] ?: 0.0
                ),
                dropoffGeoPoint = GeoPoint(
                    firebaseBooking.dropoffCoordinates["lat"] ?: 0.0,
                    firebaseBooking.dropoffCoordinates["lng"] ?: 0.0
                ),
                estimatedFare = firebaseBooking.estimatedFare,
                status = try {
                    BookingStatus.valueOf(firebaseBooking.status)
                } catch (_: IllegalArgumentException) {
                    BookingStatus.PENDING
                },
                timestamp = firebaseBooking.timestamp,
                assignedTricycleId = firebaseBooking.assignedTricycleId,
                verificationCode = firebaseBooking.verificationCode,
                driverName = firebaseBooking.driverName,
                driverRFID = firebaseBooking.driverRFID,
                todaNumber = firebaseBooking.todaNumber,
                assignedDriverId = firebaseBooking.assignedDriverId,
                // Add arrival and no-show tracking fields
                arrivedAtPickup = firebaseBooking.arrivedAtPickup,
                arrivedAtPickupTime = firebaseBooking.arrivedAtPickupTime,
                isNoShow = firebaseBooking.isNoShow,
                noShowReportedTime = firebaseBooking.noShowReportedTime
            )
        } catch (e: Exception) {
            println("Error getting booking by ID: ${e.message}")
            null
        }
    }

    // Try to match booking to first available driver in queue
    suspend fun tryMatchBookingToFirstDriver(bookingId: String): Result<Boolean> {
        return try {
            val matched = firebaseService.tryMatchBookingToFirstDriver(bookingId)
            Result.success(matched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Driver Location Management
    suspend fun updateDriverLocation(
        driverId: String,
        tricycleId: String,
        latitude: Double,
        longitude: Double,
        isOnline: Boolean,
        isAvailable: Boolean,
        currentBookingId: String? = null
    ): Result<Unit> {
        return try {
            val location = FirebaseDriverLocation(
                driverId = driverId,
                tricycleId = tricycleId,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                isOnline = isOnline,
                isAvailable = isAvailable,
                currentBookingId = currentBookingId
            )

            val success = firebaseService.updateDriverLocation(location)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAvailableDrivers(): Flow<List<AvailableDriver>> {
        return firebaseService.getAvailableDrivers().map { driversMap ->
            driversMap.map { (driverId, driverData) ->
                AvailableDriver(
                    driverId = driverId,
                    tricycleId = driverData["tricycleId"] as? String ?: "",
                    latitude = driverData["lat"] as? Double ?: 0.0,
                    longitude = driverData["lng"] as? Double ?: 0.0,
                    timestamp = driverData["timestamp"] as? Long ?: 0L
                )
            }
        }
    }

    // Driver Registration Management
    suspend fun submitDriverApplication(driver: Driver): Result<String> {
        return try {
            println("=== SUBMITTING DRIVER APPLICATION ===")
            println("Driver: ${driver.name}")
            println("Phone: ${driver.phoneNumber}")
            println("Creating Firebase Auth account...")

            // Step 1: Create Firebase Auth account and user profile first
            val authResult = registerUser(
                phoneNumber = driver.phoneNumber,
                name = driver.name,
                userType = UserType.DRIVER,
                password = driver.password
            )

            authResult.fold(
                onSuccess = { userId ->
                    println("✓ Firebase Auth account created: $userId")

                    // Step 2: Add additional driver-specific data to drivers table
                    println("Creating driver record in database...")
                    val driverId = firebaseService.createDriverInDriversTable(
                        userId = userId, // Pass the Auth User ID
                        driverName = driver.name,
                        todaNumber = "", // Empty - admin will assign TODA number later
                        phoneNumber = driver.phoneNumber,
                        address = driver.address,
                        emergencyContact = "", // Not in registration form anymore
                        licenseNumber = driver.licenseNumber,
                        licenseExpiry = 0, // Not in registration form anymore
                        yearsOfExperience = 0, // Not in registration form anymore
                        tricyclePlateNumber = driver.tricyclePlateNumber // Add tricycle plate number
                    )

                    if (driverId != null) {
                        println("✓ Driver record created: $driverId")

                        // IMPORTANT: Sign out to allow auto-login to work properly
                        authService.signOut()
                        println("✓ Signed out from Firebase Auth to prepare for auto-login")

                        Result.success(userId) // Return the Auth User ID instead of driver ID
                    } else {
                        println("✗ Failed to create driver record")
                        Result.failure(Exception("Failed to add driver to system"))
                    }
                },
                onFailure = { exception ->
                    println("✗ Failed to create Firebase Auth account: ${exception.message}")
                    Result.failure(Exception("Failed to create driver account: ${exception.message}"))
                }
            )
        } catch (e: Exception) {
            println("✗ Exception during registration: ${e.message}")
            Result.failure(e)
        }
    }

    // New method to get all drivers for admin management
    suspend fun getAllDrivers(): Result<List<Driver>> {
        return try {
            val driversData = firebaseService.getAllDrivers()
            val drivers = driversData.map { driverData ->
                Driver(
                    id = driverData["id"] as? String ?: "",
                    name = driverData["driverName"] as? String ?: "",
                    phoneNumber = driverData["phoneNumber"] as? String ?: "",
                    address = driverData["address"] as? String ?: "",
                    licenseNumber = driverData["licenseNumber"] as? String ?: "",
                    tricyclePlateNumber = driverData["tricyclePlateNumber"] as? String ?: "",
                    password = driverData["password"] as? String ?: "",
                    rfidUID = driverData["rfidUID"] as? String ?: "",
                    todaMembershipId = driverData["todaMembershipId"] as? String ?: "",
                    isActive = driverData["isActive"] as? Boolean ?: false,
                    registrationDate = driverData["registrationDate"] as? Long ?: System.currentTimeMillis(),
                    status = DriverStatus.valueOf(driverData["status"] as? String ?: "PENDING_APPROVAL"),
                    approvedBy = driverData["approvedBy"] as? String ?: "",
                    approvalDate = driverData["approvalDate"] as? Long ?: 0,
                    hasRfidAssigned = driverData["hasRfidAssigned"] as? Boolean ?: false,
                    hasTodaMembershipAssigned = driverData["hasTodaMembershipAssigned"] as? Boolean ?: false
                )
            }
            Result.success(drivers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // New method to assign RFID to a driver
    suspend fun assignRfidToDriver(driverId: String, rfidUID: String): Result<Unit> {
        return try {
            val success = firebaseService.assignRfidToDriver(driverId, rfidUID)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to assign RFID to driver"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Chat Management
    suspend fun createOrGetChatRoom(bookingId: String, customerId: String, customerName: String, driverId: String, driverName: String): Result<String> {
        return try {
            val chatRoomId = firebaseService.createOrGetChatRoom(bookingId, customerId, customerName, driverId, driverName)
            if (chatRoomId != null) {
                Result.success(chatRoomId)
            } else {
                Result.failure(Exception("Failed to create chat room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        bookingId: String,
        senderId: String,
        senderName: String,
        receiverId: String,
        message: String
    ): Result<Unit> {
        return try {
            val chatMessage = FirebaseChatMessage(
                bookingId = bookingId,
                senderId = senderId,
                senderName = senderName,
                receiverId = receiverId,
                message = message,
                timestamp = System.currentTimeMillis(),
                messageType = "TEXT",
                isRead = false
            )

            val success = firebaseService.sendMessage(chatMessage)
            if (success) {
                // Update chat room's last message
                firebaseService.updateChatRoomLastMessage(bookingId, message)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatMessages(bookingId: String): Flow<List<ChatMessage>> {
        return firebaseService.getChatMessages(bookingId).map { firebaseMessages ->
            firebaseMessages.map { firebaseMessage ->
                ChatMessage(
                    id = firebaseMessage.id,
                    bookingId = firebaseMessage.bookingId,
                    senderId = firebaseMessage.senderId,
                    senderName = firebaseMessage.senderName,
                    receiverId = firebaseMessage.receiverId,
                    message = firebaseMessage.message,
                    timestamp = firebaseMessage.timestamp,
                    messageType = firebaseMessage.messageType,
                    isRead = firebaseMessage.isRead
                )
            }
        }
    }

    fun getChatRoom(bookingId: String): Flow<ChatRoom?> {
        return firebaseService.getChatRoom(bookingId).map { firebaseChatRoom ->
            firebaseChatRoom?.let { room ->
                ChatRoom(
                    id = room.id,
                    bookingId = room.bookingId,
                    customerId = room.customerId,
                    customerName = room.customerName,
                    driverId = room.driverId,
                    driverName = room.driverName,
                    createdAt = room.createdAt,
                    lastMessageTime = room.lastMessageTime,
                    lastMessage = room.lastMessage,
                    isActive = room.isActive
                )
            }
        }
    }

    fun getUserChatRooms(userId: String): Flow<List<ChatRoom>> {
        return firebaseService.getUserChatRooms(userId).map { firebaseChatRooms ->
            firebaseChatRooms.map { room ->
                ChatRoom(
                    id = room.id,
                    bookingId = room.bookingId,
                    customerId = room.customerId,
                    customerName = room.customerName,
                    driverId = room.driverId,
                    driverName = room.driverName,
                    createdAt = room.createdAt,
                    lastMessageTime = room.lastMessageTime,
                    lastMessage = room.lastMessage,
                    isActive = room.isActive
                )
            }
        }
    }

    // Emergency Management
    suspend fun createEmergencyAlert(
        userId: String,
        userName: String,
        bookingId: String?,
        latitude: Double,
        longitude: Double,
        message: String
    ): Result<String> {
        return try {
            val alert = FirebaseEmergencyAlert(
                userId = userId,
                userName = userName,
                bookingId = bookingId,
                location = mapOf("lat" to latitude, "lng" to longitude),
                message = message,
                timestamp = System.currentTimeMillis(),
                isResolved = false
            )

            val alertId = firebaseService.createEmergencyAlert(alert)
            if (alertId != null) {
                Result.success(alertId)
            } else {
                Result.failure(Exception("Failed to create emergency alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverInfo(driverId: String): Result<Driver?> {
        return try {
            val driverData = firebaseService.getDriverById(driverId)
            if (driverData != null) {
                val driver = Driver(
                    id = driverId,
                    name = driverData["driverName"] as? String ?: "",
                    phoneNumber = driverData["phoneNumber"] as? String ?: "",
                    address = driverData["address"] as? String ?: "",
                    licenseNumber = driverData["licenseNumber"] as? String ?: "",
                    tricyclePlateNumber = driverData["tricyclePlateNumber"] as? String ?: "",
                    password = driverData["password"] as? String ?: "",
                    rfidUID = driverData["rfidUID"] as? String ?: "",
                    todaMembershipId = driverData["todaMembershipId"] as? String ?: "",
                    isActive = driverData["isActive"] as? Boolean ?: false,
                    registrationDate = driverData["registrationDate"] as? Long ?: System.currentTimeMillis(),
                    status = DriverStatus.valueOf(driverData["status"] as? String ?: "PENDING_APPROVAL"),
                    approvedBy = driverData["approvedBy"] as? String ?: "",
                    approvalDate = driverData["approvalDate"] as? Long ?: 0,
                    hasRfidAssigned = driverData["hasRfidAssigned"] as? Boolean ?: false,
                    hasTodaMembershipAssigned = driverData["hasTodaMembershipAssigned"] as? Boolean ?: false
                )
                Result.success(driver)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fix for existing drivers who have isVerified = false
    suspend fun fixDriverVerificationStatus(phoneNumber: String): Result<Unit> {
        return try {
            val success = firebaseService.fixDriverVerificationStatus(phoneNumber)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fix driver verification status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // New method to check driver application status
    suspend fun getDriverApplicationStatus(phoneNumber: String): Result<String> {
        return try {
            val status = firebaseService.getDriverApplicationStatus(phoneNumber)
            if (status != null) {
                Result.success(status)
            } else {
                Result.failure(Exception("No driver application found for this phone number"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverContributionStatus(driverId: String): Result<Boolean> {
        return try {
            val hasContributed = firebaseService.getDriverContributionStatus(driverId)
            Result.success(hasContributed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isDriverInQueue(driverRFID: String): Result<Boolean> {
        return try {
            val isInQueue = firebaseService.isDriverInQueue(driverRFID)
            Result.success(isInQueue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeDriverQueueStatus(driverRFID: String): Flow<Boolean> {
        return firebaseService.observeDriverQueueStatus(driverRFID)
    }

    fun observeDriverQueue(): Flow<List<QueueEntry>> {
        return firebaseService.observeDriverQueue()
    }

    suspend fun leaveQueue(driverRFID: String): Result<Boolean> {
        return try {
            val success = firebaseService.leaveQueue(driverRFID)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverTodayStats(driverId: String): Result<Triple<Int, Double, Double>> {
        return try {
            val stats = firebaseService.getDriverTodayStats(driverId)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverById(driverId: String): Result<Map<String, Any>> {
        return try {
            val driverData = firebaseService.getDriverById(driverId)
            if (driverData != null) {
                Result.success(driverData)
            } else {
                Result.failure(Exception("Driver not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Driver Contributions Management
    suspend fun getDriverContributions(driverId: String): List<FirebaseContribution> {
        return try {
            firebaseService.getDriverContributions(driverId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDriverTodayContributions(driverId: String): List<FirebaseContribution> {
        return try {
            firebaseService.getDriverTodayContributions(driverId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDriverContributionSummary(driverId: String): ContributionSummary {
        return try {
            val contributions = firebaseService.getDriverContributions(driverId)
            calculateContributionSummary(contributions)
        } catch (e: Exception) {
            ContributionSummary()
        }
    }

    private fun calculateContributionSummary(contributions: List<FirebaseContribution>): ContributionSummary {
        if (contributions.isEmpty()) return ContributionSummary()

        val now = System.currentTimeMillis()
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val weekStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val totalContributions = contributions.sumOf { it.amount }
        val todayContributions = contributions.filter { it.timestamp >= todayStart }.sumOf { it.amount }
        val thisWeekContributions = contributions.filter { it.timestamp >= weekStart }.sumOf { it.amount }
        val thisMonthContributions = contributions.filter { it.timestamp >= monthStart }.sumOf { it.amount }

        val sortedContributions = contributions.sortedByDescending { it.timestamp }
        val lastContributionDate = sortedContributions.firstOrNull()?.timestamp

        // Calculate streak
        val streak = calculateContributionStreak(contributions)

        // Calculate average daily contribution (last 30 days)
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)
        val recentContributions = contributions.filter { it.timestamp >= thirtyDaysAgo }
        val averageDailyContribution = if (recentContributions.isNotEmpty()) {
            recentContributions.sumOf { it.amount } / 30.0
        } else 0.0

        return ContributionSummary(
            totalContributions = totalContributions,
            todayContributions = todayContributions,
            thisWeekContributions = thisWeekContributions,
            thisMonthContributions = thisMonthContributions,
            contributionCount = contributions.size,
            lastContributionDate = lastContributionDate,
            averageDailyContribution = averageDailyContribution,
            streak = streak
        )
    }

    private fun calculateContributionStreak(contributions: List<FirebaseContribution>): Int {
        if (contributions.isEmpty()) return 0

        val sortedContributions = contributions.sortedByDescending { it.timestamp }
        val contributionDates = sortedContributions.map { contribution ->
            java.util.Calendar.getInstance().apply {
                timeInMillis = contribution.timestamp
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.distinct().sorted()

        if (contributionDates.isEmpty()) return 0

        var streak = 1
        val oneDayMillis = 24 * 60 * 60 * 1000L

        for (i in contributionDates.size - 2 downTo 0) {
            val currentDate = contributionDates[i]
            val nextDate = contributionDates[i + 1]

            if (nextDate - currentDate == oneDayMillis) {
                streak++
            } else {
                break
            }
        }

        return streak
    }

    // Helper functions
    private fun generateUserId(): String = "user_${System.currentTimeMillis()}_${(1000..9999).random()}"

    // RFID Management methods
    suspend fun reportMissingRfid(driverId: String, reason: String): Result<Unit> {
        return try {
            val success = firebaseService.reportMissingRfid(driverId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to report missing RFID"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRfidChangeHistory(driverId: String): Result<List<RfidChangeHistory>> {
        return try {
            val historyFlow = firebaseService.getRfidChangeHistory(driverId)
            val historyMaps = historyFlow.first()

            val historyList = historyMaps.map { map ->
                RfidChangeHistory(
                    id = map["id"] as? String ?: "",
                    driverId = map["driverId"] as? String ?: "",
                    driverName = map["driverName"] as? String ?: "",
                    oldRfidUID = map["oldRfidUID"] as? String ?: "",
                    newRfidUID = map["newRfidUID"] as? String ?: "",
                    changeType = try {
                        RfidChangeType.valueOf(map["changeType"] as? String ?: "ASSIGNED")
                    } catch (e: Exception) {
                        RfidChangeType.ASSIGNED
                    },
                    reason = map["reason"] as? String ?: "",
                    changedBy = map["changedBy"] as? String ?: "",
                    changedByName = map["changedByName"] as? String ?: "",
                    timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    notes = map["notes"] as? String ?: ""
                )
            }

            Result.success(historyList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Link email/password to the currently authenticated user (after phone OTP) and create profile
    suspend fun registerUserByLinkingEmailToCurrentUser(
        email: String,
        phoneNumber: String,
        password: String,
        userData: Map<String, Any>
    ): Result<FirebaseUser> {
        return try {
            val linkResult = authService.linkEmailPasswordToCurrentUser(email, password)
            linkResult.fold(
                onSuccess = { userId ->
                    val user = FirebaseUser(
                        id = userId,
                        phoneNumber = phoneNumber,
                        name = userData["name"] as? String ?: "",
                        userType = userData["userType"] as? String ?: "PASSENGER",
                        isVerified = userData["verified"] as? Boolean ?: true,
                        registrationDate = userData["registrationDate"] as? Long ?: System.currentTimeMillis()
                    )

                    val updatedUserData = userData + ("id" to userId)
                    val success = firebaseService.createUserWithData(userId, updatedUserData)
                    if (success) Result.success(user) else Result.failure(Exception("Failed to create user profile"))
                },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return authService.sendPasswordResetEmail(email)
    }
}

// Additional data classes for repository
data class AvailableDriver(
    val driverId: String,
    val tricycleId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

// Hardware integration data classes
data class HardwareDriver(
    val rfidUID: String,
    val name: String,
    val licenseNumber: String,
    val tricycleId: String,
    val todaNumber: String,
    val isRegistered: Boolean,
    val totalContributions: Double,
    val isActive: Boolean
)

data class DriverContribution(
    val driverId: String,
    val driverName: String,
    val amount: Double,
    val timestamp: Long,
    val source: String
)
