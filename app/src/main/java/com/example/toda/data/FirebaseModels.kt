package com.example.toda.data

import com.google.firebase.database.IgnoreExtraProperties

// Firebase-optimized data models
@IgnoreExtraProperties
data class FirebaseUser(
    val id: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val userType: String = "PASSENGER", // PASSENGER, DRIVER, OPERATOR, TODA_ADMIN
    val isVerified: Boolean = false,
    val registrationDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val lastActiveTime: Long = System.currentTimeMillis(),
    // TODA membership fields
    val todaId: String? = null,
    val membershipNumber: String? = null,
    val membershipStatus: String = "ACTIVE",
    // Admin-specific fields (optional, only used for admin users)
    val address: String = "",
    val emergencyContact: String = "",
    val employeeId: String = "",
    val position: String = ""
)

@IgnoreExtraProperties
data class FirebaseUserProfile(
    val phoneNumber: String = "",
    val name: String = "",
    val userType: String = "PASSENGER",
    val address: String = "",
    val emergencyContact: String = "",
    val profilePicture: String? = null,
    // Passenger specific
    val totalBookings: Int = 0,
    val completedBookings: Int = 0,
    val cancelledBookings: Int = 0,
    val trustScore: Double = 100.0,
    val isBlocked: Boolean = false,
    val lastBookingTime: Long = 0,
    // Discount eligibility
    val discountType: String? = null, // PWD, SENIOR_CITIZEN, STUDENT
    val discountIdNumber: String = "",
    val discountIdImageUrl: String = "", // URL to uploaded ID image in Firebase Storage
    val discountVerified: Boolean = false,
    val discountExpiryDate: Long? = null,
    // Driver specific
    val licenseNumber: String? = null,
    val licenseExpiry: Long? = null,
    val yearsOfExperience: Int = 0,
    val rating: Double = 5.0,
    val totalTrips: Int = 0,
    val earnings: Double = 0.0,
    val isOnline: Boolean = false,
    val currentLocation: Map<String, Double>? = null // lat, lng
)

@IgnoreExtraProperties
data class FirebaseBooking(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val phoneNumber: String = "",
    val isPhoneVerified: Boolean = false,
    val pickupLocation: String = "",
    val destination: String = "",
    val pickupCoordinates: Map<String, Double> = emptyMap(), // lat, lng
    val dropoffCoordinates: Map<String, Double> = emptyMap(), // lat, lng
    val estimatedFare: Double = 0.0,
    val actualFare: Double = 0.0,
    val status: String = "PENDING", // PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    val timestamp: Long = System.currentTimeMillis(),
    val assignedDriverId: String = "",
    val assignedTricycleId: String = "",
    val verificationCode: String = "",
    val completionTime: String = "0", // Changed to String to handle Firebase data
    val rating: Double = 0.0,
    val feedback: String = "",
    val paymentMethod: String = "CASH",
    val distance: Double = 0.0,
    val duration: Int = 0, // in minutes
    // Additional fields that might be present in Firebase
    val driverName: String = "",
    val driverRFID: String = "",
    val todaNumber: String = "",
    // New field to classify trip creation source
    val tripType: String = "",
    // Pickup arrival and no-show tracking
    val arrivedAtPickup: Boolean = false,
    val arrivedAtPickupTime: Long = 0L,
    val isNoShow: Boolean = false,
    val noShowReportedTime: Long = 0L
) {
    // Helper function to get completionTime as Long
    fun getCompletionTimeLong(): Long {
        return try {
            completionTime.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    // Helper function to get phone verification status
    fun isPhoneActuallyVerified(): Boolean {
        return isPhoneVerified
    }
}

@IgnoreExtraProperties
data class FirebaseTricycle(
    val id: String = "",
    val plateNumber: String = "",
    val todaNumber: String = "",
    val bodyNumber: String = "",
    val engineNumber: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val ownerName: String = "",
    val ownerContact: String = "",
    val isActive: Boolean = true,
    val primaryDriverId: String = "",
    val registeredDrivers: Map<String, Boolean> = emptyMap(), // driverId -> true
    val currentDriverId: String? = null,
    val lastMaintenanceDate: Long = 0,
    val nextMaintenanceDate: Long = 0,
    val todaOrganizationId: String = "",
    val currentLocation: Map<String, Double>? = null, // lat, lng
    val isOnline: Boolean = false,
    val totalTrips: Int = 0,
    val totalEarnings: Double = 0.0
)

@IgnoreExtraProperties
data class FirebaseDriverRegistration(
    val id: String = "",
    val applicantName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val emergencyContact: String = "",
    val licenseNumber: String = "",
    val licenseExpiry: Long = 0,
    val yearsOfExperience: Int = 0,
    val tricycleId: String = "",
    val todaNumber: String = "",
    val applicationDate: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED, UNDER_REVIEW
    val approvedBy: String? = null,
    val approvalDate: Long? = null,
    val rejectionReason: String? = null,
    // Document verification
    val documents: Map<String, Boolean> = mapOf(
        "validLicense" to false,
        "barangayClearance" to false,
        "policeClearance" to false,
        "medicalCertificate" to false,
        "driverTrainingCertificate" to false
    )
)

@IgnoreExtraProperties
data class FirebaseTODAOrganization(
    val id: String = "",
    val name: String = "",
    val registrationNumber: String = "",
    val address: String = "",
    val contactNumber: String = "",
    val presidentName: String = "",
    val isActive: Boolean = true,
    val registrationDate: Long = System.currentTimeMillis(),
    val serviceArea: String = "Barangay 177, Caloocan City",
    val totalMembers: Int = 0,
    val totalTricycles: Int = 0,
    val totalBookings: Int = 0
)

@IgnoreExtraProperties
data class FirebaseDriverLocation(
    val driverId: String = "",
    val tricycleId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Double = 0.0,
    val speed: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val isAvailable: Boolean = false,
    val currentBookingId: String? = null
)

@IgnoreExtraProperties
data class FirebaseChatMessage(
    val id: String = "",
    val bookingId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT", // TEXT, IMAGE, LOCATION
    val isRead: Boolean = false
)

@IgnoreExtraProperties
data class FirebaseChatRoom(
    val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastMessage: String = "",
    val isActive: Boolean = true
)

@IgnoreExtraProperties
data class FirebaseNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // INFO, BOOKING, EMERGENCY, SYSTEM
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val data: Map<String, String> = emptyMap()
)

@IgnoreExtraProperties
data class FirebaseEmergencyAlert(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val bookingId: String? = null,
    val location: Map<String, Double> = emptyMap(), // lat, lng
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val respondedBy: String? = null,
    val responseTime: Long? = null
)

// New models for hardware integration
@IgnoreExtraProperties
data class FirebaseHardwareDriver(
    val rfidUID: String = "",
    val name: String = "",
    val licenseNumber: String = "",
    val tricycleId: String = "",
    val todaNumber: String = "",
    val isRegistered: Boolean = false,
    val createdBy: String = "hardware", // "hardware" | "mobile"
    val registrationDate: Long = System.currentTimeMillis(),
    val totalContributions: Double = 0.0,
    val isActive: Boolean = true
)

@IgnoreExtraProperties
data class FirebaseDriverQueueEntry(
    val driverId: String = "", // RFID UID
    val driverName: String = "",
    val queuePosition: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val contributionAmount: Double = 5.0,
    val status: String = "waiting" // "waiting" | "assigned" | "completed"
)

@IgnoreExtraProperties
data class FirebaseContribution(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverRFID: String = "", // Add this to match Firebase field name
    val rfidUID: String = "",
    val amount: Double = 5.0,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "",
    val contributionType: String = "COIN_INSERTION", // COIN_INSERTION, MANUAL, ADMIN_ADJUSTMENT
    val notes: String = "",
    val deviceId: String = "", // Which coin insertion device recorded this
    val verified: Boolean = true,
    val source: String = "hardware", // "hardware" | "mobile"
    val paid: Boolean = true // false indicates pay_later contribution
)

@IgnoreExtraProperties
data class FirebaseActiveTrip(
    val tripId: String = "",
    val driverId: String = "",
    val bookingId: String? = null, // null if hardware-assigned
    val passengerType: String = "regular", // "regular" | "special"
    val assignmentSource: String = "hardware", // "hardware" | "mobile"
    val startTime: Long = System.currentTimeMillis(),
    val status: String = "active", // "active" | "completed"

    // Hardware assignment data
    val buttonPressed: String? = null, // "regular" | "special"

    // Mobile booking data (if applicable)
    val customerId: String? = null,
    val pickupLocation: String? = null,
    val destination: String? = null
)

@IgnoreExtraProperties
data class FirebaseSystemStatus(
    val component: String = "",
    val status: String = "offline", // "online" | "offline"
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val additionalData: Map<String, Any> = emptyMap()
)

@IgnoreExtraProperties
data class UnifiedQueueEntry(
    val driverId: String = "",
    val driverName: String = "",
    val queuePosition: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "hardware", // "hardware" | "mobile"
    val isInPhysicalQueue: Boolean = true
)

@IgnoreExtraProperties
data class FirebaseRating(
    val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val stars: Int = 0,
    val feedback: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ratedBy: String = "DRIVER" // "DRIVER" or "CUSTOMER"
)

data class LoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val userId: String? = null
)
