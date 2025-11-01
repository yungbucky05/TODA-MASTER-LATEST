package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.*
import com.example.toda.repository.TODARepository
import com.example.toda.repository.AvailableDriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@HiltViewModel
class EnhancedBookingViewModel @Inject constructor(
    private val repository: TODARepository
) : ViewModel() {

    private val _bookingState = MutableStateFlow(BookingState())
    val bookingState = _bookingState.asStateFlow()

    private val _availableDrivers = MutableStateFlow<List<AvailableDriver>>(emptyList())
    val availableDrivers = _availableDrivers.asStateFlow()

    private val _activeBookings = MutableStateFlow<List<Booking>>(emptyList())
    val activeBookings = _activeBookings.asStateFlow()

    private val _queueList = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queueList = _queueList.asStateFlow()

    // Polling jobs keyed by bookingId to avoid duplicates
    private val pollingJobs: MutableMap<String, Job> = mutableMapOf()

    // No-show monitoring jobs keyed by bookingId
    private val noShowMonitorJobs: MutableMap<String, Job> = mutableMapOf()

    // No-show timeout in milliseconds (5 minutes)
    private val NO_SHOW_TIMEOUT_MS = 5 * 60 * 1000L

    init {
        // Observe available drivers in real-time
        viewModelScope.launch {
            repository.getAvailableDrivers().collect { drivers ->
                _availableDrivers.value = drivers
            }
        }

        // Observe active bookings in real-time (for operators)
        viewModelScope.launch {
            repository.getActiveBookings().collect { bookings ->
                _activeBookings.value = bookings

                // Monitor bookings for driver arrival and auto-start no-show countdown
                bookings.forEach { booking ->
                    if (booking.arrivedAtPickup &&
                        booking.arrivedAtPickupTime > 0L &&
                        booking.status == BookingStatus.ACCEPTED &&
                        !noShowMonitorJobs.containsKey(booking.id)) {
                        // Driver has arrived, start automatic no-show monitoring
                        println("📍 Auto-starting no-show monitoring for booking ${booking.id}")
                        startNoShowMonitoring(booking.id, booking.arrivedAtPickupTime)
                    }
                }
            }
        }

        // Observe driver queue in real-time
        viewModelScope.launch {
            repository.observeDriverQueue().collect { queueEntries ->
                _queueList.value = queueEntries
            }
        }
    }

    fun createBooking(
        customerId: String,
        customerName: String,
        phoneNumber: String,
        pickupLocation: String,
        destination: String,
        pickupGeoPoint: GeoPoint,
        dropoffGeoPoint: GeoPoint,
        estimatedFare: Double
    ) {
        viewModelScope.launch {
            println("=== VIEWMODEL CREATE BOOKING DEBUG ===")
            println("Input parameters:")
            println("  customerId: $customerId")
            println("  customerName: $customerName")
            println("  phoneNumber: $phoneNumber")
            println("  pickupLocation: $pickupLocation")
            println("  destination: $destination")
            println("  pickupGeoPoint: $pickupGeoPoint")
            println("  dropoffGeoPoint: $dropoffGeoPoint")
            println("  estimatedFare: $estimatedFare")

            _bookingState.value = _bookingState.value.copy(isLoading = true)

            val booking = Booking(
                customerId = customerId,
                customerName = customerName,
                phoneNumber = phoneNumber,
                isPhoneVerified = true, // Assume verified
                pickupLocation = pickupLocation,
                destination = destination,
                pickupGeoPoint = pickupGeoPoint,
                dropoffGeoPoint = dropoffGeoPoint,
                estimatedFare = estimatedFare,
                status = BookingStatus.PENDING,
                timestamp = System.currentTimeMillis(),
                verificationCode = (100000..999999).random().toString()
            )

            println("Created booking object: $booking")
            println("Calling repository.createBooking...")

            repository.createBooking(booking).fold(
                onSuccess = { bookingId ->
                    println("SUCCESS: ViewModel received booking ID: $bookingId")
                    _bookingState.value = _bookingState.value.copy(
                        isLoading = false,
                        currentBookingId = bookingId,
                        message = "Booking created successfully! Waiting for driver acceptance."
                    )

                    // Start 10-second polling to retry assignment until matched or timeout
                    startBookingPolling(bookingId)
                },
                onFailure = { error ->
                    println("ERROR: ViewModel received error: ${error.message}")
                    println("Error type: ${error::class.java.simpleName}")
                    error.printStackTrace()
                    _bookingState.value = _bookingState.value.copy(
                        isLoading = false,
                        error = "Failed to create booking: ${error.message}"
                    )
                }
            )
        }
    }

    private fun startBookingPolling(bookingId: String, intervalMs: Long = 10_000L, maxAttempts: Int = 12) {
        if (pollingJobs.containsKey(bookingId)) {
            println("Polling already active for $bookingId; skipping duplicate start")
            return
        }
        val job = viewModelScope.launch {
            println("Starting polling for booking $bookingId (every ${intervalMs}ms, up to $maxAttempts attempts)")
            var attempts = 0
            while (attempts < maxAttempts) {
                attempts++
                try {
                    val current = repository.getBookingByIdOnce(bookingId)
                    if (current == null) {
                        println("Polling: booking $bookingId not found (attempt $attempts)")
                    } else {
                        println("Polling: booking $bookingId status=${current.status} assignedDriverId='${current.assignedDriverId}' (attempt $attempts)")
                        // Stop polling if no longer pending
                        if (current.status != BookingStatus.PENDING) {
                            println("Booking $bookingId no longer pending; stopping polling")
                            break
                        }
                        // If still pending, try to match to first driver in queue
                        repository.tryMatchBookingToFirstDriver(bookingId).fold(
                            onSuccess = { matched ->
                                if (matched) {
                                    println("Polling: matched booking $bookingId to a driver; will verify on next tick or via realtime stream")
                                } else {
                                    println("Polling: no match yet for $bookingId; will try again")
                                }
                            },
                            onFailure = { e ->
                                println("Polling: error trying to match booking $bookingId: ${e.message}")
                            }
                        )
                    }
                } catch (e: Exception) {
                    println("Polling exception for $bookingId: ${e.message}")
                }
                delay(intervalMs)
            }
            println("Stopping polling for booking $bookingId after $attempts attempts")
            pollingJobs.remove(bookingId)
        }
        pollingJobs[bookingId] = job
    }

    fun stopBookingPolling(bookingId: String) {
        pollingJobs.remove(bookingId)?.cancel()
        println("Manually stopped polling for booking $bookingId")
    }

    // Start monitoring for no-show after driver arrives
    fun startNoShowMonitoring(bookingId: String, arrivedAtPickupTime: Long) {
        // Cancel existing monitoring for this booking if any
        noShowMonitorJobs[bookingId]?.cancel()

        val job = viewModelScope.launch {
            val timeElapsed = System.currentTimeMillis() - arrivedAtPickupTime
            val remainingTime = NO_SHOW_TIMEOUT_MS - timeElapsed

            if (remainingTime > 0) {
                println("Starting no-show monitoring for booking $bookingId, waiting ${remainingTime}ms")
                delay(remainingTime)

                // Check if booking is still in ACCEPTED status (not started)
                val booking = repository.getBookingByIdOnce(bookingId)
                if (booking != null && booking.status == BookingStatus.ACCEPTED && booking.arrivedAtPickup) {
                    println("Auto-reporting no-show for booking $bookingId after timeout")
                    reportNoShow(bookingId)
                }
            } else {
                println("Timeout already passed for booking $bookingId, reporting no-show immediately")
                reportNoShow(bookingId)
            }
        }

        noShowMonitorJobs[bookingId] = job
    }

    // Stop monitoring (called when trip starts or is cancelled)
    fun stopNoShowMonitoring(bookingId: String) {
        noShowMonitorJobs[bookingId]?.cancel()
        noShowMonitorJobs.remove(bookingId)
        println("Stopped no-show monitoring for booking $bookingId")
    }

    // Clear all monitoring when view model is cleared
    override fun onCleared() {
        super.onCleared()
        noShowMonitorJobs.values.forEach { it.cancel() }
        noShowMonitorJobs.clear()
    }

    fun acceptBooking(bookingId: String, driverId: String) {
        viewModelScope.launch {
            repository.acceptBooking(bookingId, driverId).fold(
                onSuccess = {
                    _bookingState.value = _bookingState.value.copy(
                        message = "Booking accepted successfully!"
                    )
                    // Create chat room after accepting booking (inline)
                    viewModelScope.launch {
                        try {
                            val booking = repository.getBookingByIdOnce(bookingId)
                            if (booking == null) {
                                println("createChatRoom: Booking not found for id=$bookingId")
                            } else {
                                val driverName = try {
                                    val driverMapResult = repository.getDriverById(driverId)
                                    driverMapResult.getOrNull()?.let { map ->
                                        (map["name"] as? String)
                                            ?: (map["driverName"] as? String)
                                            ?: "Driver"
                                    } ?: "Driver"
                                } catch (e: Exception) {
                                    "Driver"
                                }
                                repository.createOrGetChatRoom(
                                    bookingId = bookingId,
                                    customerId = booking.customerId,
                                    customerName = booking.customerName,
                                    driverId = driverId,
                                    driverName = driverName
                                ).fold(
                                    onSuccess = { roomId ->
                                        println("Chat room ready for booking=$bookingId (roomId=$roomId)")
                                    },
                                    onFailure = { e ->
                                        println("Failed to create/get chat room for booking=$bookingId: ${e.message}")
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            println("createChatRoom inline exception: ${e.message}")
                        }
                    }
                },
                onFailure = { error ->
                    _bookingState.value = _bookingState.value.copy(
                        error = "Failed to accept booking: ${error.message}"
                    )
                }
            )
        }
    }

    fun updateBookingStatusOnly(bookingId: String, status: String) {
        viewModelScope.launch {
            repository.updateBookingStatusOnly(bookingId, status).fold(
                onSuccess = {
                    _bookingState.value = _bookingState.value.copy(
                        message = "Booking status updated to $status successfully!"
                    )

                    // Create rating entry when trip is completed
                    if (status == "COMPLETED") {
                        repository.createRatingEntry(bookingId).fold(
                            onSuccess = {
                                println("✓ Rating entry created for booking: $bookingId")
                            },
                            onFailure = { error ->
                                println("✗ Failed to create rating entry: ${error.message}")
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _bookingState.value = _bookingState.value.copy(
                        error = "Failed to update booking status: ${error.message}"
                    )
                }
            )
        }
    }

    fun updateDriverLocation(
        driverId: String,
        tricycleId: String,
        latitude: Double,
        longitude: Double,
        isOnline: Boolean,
        isAvailable: Boolean
    ) {
        viewModelScope.launch {
            repository.updateDriverLocation(
                driverId, tricycleId, latitude, longitude, isOnline, isAvailable
            ).fold(
                onSuccess = {
                    // Location updated successfully
                },
                onFailure = { error ->
                    _bookingState.value = _bookingState.value.copy(
                        error = "Failed to update location: ${error.message}"
                    )
                }
            )
        }
    }

    fun sendMessage(
        bookingId: String,
        senderId: String,
        senderName: String,
        receiverId: String,
        message: String
    ) {
        viewModelScope.launch {
            println("🎯 ViewModel: Sending message")
            println("   BookingId: $bookingId")
            println("   From: $senderName ($senderId)")
            println("   To: $receiverId")
            println("   Message: $message")

            val result = repository.sendChatMessage(bookingId, senderId, senderName, receiverId, message)
            result.fold(
                onSuccess = {
                    println("✅ ViewModel: Message sent successfully")
                },
                onFailure = { error ->
                    println("❌ ViewModel: Failed to send message: ${error.message}")
                }
            )
        }
    }

    fun createEmergencyAlert(
        userId: String,
        userName: String,
        bookingId: String?,
        latitude: Double,
        longitude: Double,
        message: String
    ) {
        viewModelScope.launch {
            repository.createEmergencyAlert(
                userId = userId,
                userName = userName,
                bookingId = bookingId,
                latitude = latitude,
                longitude = longitude,
                message = message
            ).fold(
                onSuccess = { alertId ->
                    _bookingState.value = _bookingState.value.copy(
                        message = "Emergency alert sent! Alert ID: $alertId"
                    )
                },
                onFailure = { error ->
                    _bookingState.value = _bookingState.value.copy(
                        error = "Failed to send emergency alert: ${error.message}"
                    )
                }
            )
        }
    }

    // Driver-specific methods for DriverInterface
    suspend fun getDriverById(driverId: String): Result<Map<String, Any>> {
        return repository.getDriverById(driverId)
    }

    suspend fun getDriverContributionStatus(driverId: String): Result<Boolean> {
        return repository.getDriverContributionStatus(driverId)
    }

    suspend fun isDriverInQueue(driverRFID: String): Result<Boolean> {
        return repository.isDriverInQueue(driverRFID)
    }

    fun observeDriverQueueStatus(driverRFID: String): Flow<Boolean> {
        return repository.observeDriverQueueStatus(driverRFID)
    }

    suspend fun leaveQueue(driverRFID: String): Result<Boolean> {
        return repository.leaveQueue(driverRFID)
    }

    suspend fun getDriverTodayStats(driverId: String): Result<Triple<Int, Double, Double>> {
        return repository.getDriverTodayStats(driverId)
    }

    // Rating submission function
    suspend fun submitRating(bookingId: String, stars: Int, feedback: String): Result<Unit> {
        return repository.updateRating(bookingId, stars, feedback)
    }

    // Enhanced Chat methods
    suspend fun sendChatMessage(
        bookingId: String,
        senderId: String,
        senderName: String,
        receiverId: String,
        message: String
    ): Result<Unit> {
        return repository.sendChatMessage(bookingId, senderId, senderName, receiverId, message)
    }

    fun getChatMessages(bookingId: String): Flow<List<ChatMessage>> {
        return repository.getChatMessages(bookingId)
    }

    fun getChatRoom(bookingId: String): Flow<ChatRoom?> {
        return repository.getChatRoom(bookingId)
    }

    fun getUserChatRooms(userId: String): Flow<List<ChatRoom>> {
        return repository.getUserChatRooms(userId)
    }

    // Add method to fetch user profile data including discount information
    fun getUserProfile(userId: String): Flow<UserProfile?> {
        return repository.getUserProfile(userId)
    }

    suspend fun createOrGetChatRoom(
        bookingId: String,
        customerId: String,
        customerName: String,
        driverId: String,
        driverName: String
    ): Result<String> {
        return repository.createOrGetChatRoom(bookingId, customerId, customerName, driverId, driverName)
    }

    fun clearMessage() {
        _bookingState.value = _bookingState.value.copy(message = null)
    }

    fun clearError() {
        _bookingState.value = _bookingState.value.copy(error = null)
    }

    // RFID Management methods
    suspend fun reportMissingRfid(driverId: String, reason: String): Result<Unit> {
        return repository.reportMissingRfid(driverId, reason)
    }

    suspend fun getRfidChangeHistory(driverId: String): Result<List<RfidChangeHistory>> {
        return repository.getRfidChangeHistory(driverId)
    }

    // Booking arrival and no-show methods
    suspend fun markArrivedAtPickup(bookingId: String): Result<Unit> {
        return repository.markArrivedAtPickup(bookingId)
    }

    suspend fun reportNoShow(bookingId: String): Result<Unit> {
        return repository.reportNoShow(bookingId)
    }

    // =====================
    // Contributions wrappers
    // =====================
    suspend fun getDriverContributions(driverId: String): Result<List<FirebaseContribution>> {
        return runCatching { repository.getDriverContributions(driverId) }
    }

    suspend fun getDriverTodayContributions(driverId: String): Result<List<FirebaseContribution>> {
        return runCatching { repository.getDriverTodayContributions(driverId) }
    }

    suspend fun getDriverContributionSummary(driverId: String): Result<ContributionSummary> {
        return runCatching { repository.getDriverContributionSummary(driverId) }
    }
}

data class BookingState(
    val isLoading: Boolean = false,
    val currentBookingId: String? = null,
    val message: String? = null,
    val error: String? = null
)
