package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.BookingStatus
import com.example.toda.data.DiscountType
import com.example.toda.data.UserProfile
import com.example.toda.repository.TODARepository
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CustomerDashboardViewModel @Inject constructor(
    private val repository: TODARepository
) : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val bookingsRef = database.getReference("bookings")

    private val _dashboardState = MutableStateFlow(CustomerDashboardState())
    val dashboardState = _dashboardState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _rideSummary = MutableStateFlow(RideSummaryCounts())
    val rideSummary = _rideSummary.asStateFlow()

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                _dashboardState.value = _dashboardState.value.copy(isLoading = true)

                // Load user data directly from Firebase to ensure discount fields are loaded
                val userSnapshot = usersRef.child(userId).get().await()
                if (userSnapshot.exists()) {
                    val userData = userSnapshot.value as? Map<*, *>

                    // Parse discount type from Firebase
                    val discountTypeValue = userData?.get("discountType")
                    val discountType = when (discountTypeValue) {
                        "PWD" -> DiscountType.PWD
                        "SENIOR_CITIZEN" -> DiscountType.SENIOR_CITIZEN
                        "STUDENT" -> DiscountType.STUDENT
                        else -> null
                    }

                    // Create UserProfile with discount data
                    val profile = UserProfile(
                        phoneNumber = userData?.get("phoneNumber") as? String ?: "",
                        name = userData?.get("name") as? String ?: "",
                        address = userData?.get("address") as? String ?: "",
                        emergencyContact = userData?.get("emergencyContact") as? String ?: "",
                        totalBookings = (userData?.get("totalBookings") as? Long)?.toInt() ?: 0,
                        completedBookings = (userData?.get("completedBookings") as? Long)?.toInt() ?: 0,
                        cancelledBookings = (userData?.get("cancelledBookings") as? Long)?.toInt() ?: 0,
                        trustScore = (userData?.get("trustScore") as? Double) ?: 100.0,
                        isBlocked = userData?.get("isBlocked") as? Boolean ?: false,
                        discountType = discountType,
                        discountIdNumber = userData?.get("discountIdNumber") as? String ?: "",
                        discountIdImageUrl = userData?.get("discountIdImageUrl") as? String ?: "",
                        discountVerified = userData?.get("discountVerified") as? Boolean ?: false,
                        discountExpiryDate = userData?.get("discountExpiryDate") as? Long
                    )

                    _userProfile.value = profile
                }

                // Load bookings belonging to this customer to compute ride summary
                val bookingsSnapshot = bookingsRef
                    .orderByChild("customerId")
                    .equalTo(userId)
                    .get()
                    .await()

                var completedCount = 0
                var cancelledCount = 0
                var ongoingCount = 0

                bookingsSnapshot.children.forEach { bookingSnapshot ->
                    val statusValue = bookingSnapshot
                        .child("status")
                        .getValue(String::class.java)
                        ?.uppercase()
                        ?: BookingStatus.PENDING.name

                    val status = runCatching { BookingStatus.valueOf(statusValue) }
                        .getOrDefault(BookingStatus.PENDING)

                    when (status) {
                        BookingStatus.COMPLETED -> completedCount++
                        BookingStatus.CANCELLED,
                        BookingStatus.NO_SHOW,
                        BookingStatus.REJECTED -> cancelledCount++
                        BookingStatus.PENDING,
                        BookingStatus.ACCEPTED,
                        BookingStatus.AT_PICKUP,
                        BookingStatus.IN_PROGRESS -> ongoingCount++
                    }
                }

                _rideSummary.value = RideSummaryCounts(
                    completed = completedCount,
                    ongoing = ongoingCount,
                    cancelled = cancelledCount
                )

                // Reflect the computed counts in the profile state for consistency across the app
                val currentProfile = _userProfile.value
                if (currentProfile != null) {
                    val totalCount = completedCount + cancelledCount + ongoingCount
                    _userProfile.value = currentProfile.copy(
                        totalBookings = totalCount,
                        completedBookings = completedCount,
                        cancelledBookings = cancelledCount
                    )
                }

                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    userId = userId
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = "Failed to load user data: ${e.message}"
                )
            }
        }
    }

    fun applyForDiscount(userId: String, discountType: DiscountType, idNumber: String, imageUrl: String = "") {
        viewModelScope.launch {
            try {
                // Save discount data directly to Firebase
                val updates = mapOf(
                    "discountType" to discountType.name, // Save as "PWD", "SENIOR_CITIZEN", or "STUDENT"
                    "discountIdNumber" to idNumber,
                    "discountIdImageUrl" to imageUrl,
                    "discountVerified" to false,
                    "discountExpiryDate" to null
                )

                usersRef.child(userId).updateChildren(updates).await()

                // Update local state
                val currentProfile = _userProfile.value
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(
                        discountType = discountType,
                        discountIdNumber = idNumber,
                        discountIdImageUrl = imageUrl,
                        discountVerified = false,
                        discountExpiryDate = null
                    )
                    _userProfile.value = updatedProfile
                }

                _dashboardState.value = _dashboardState.value.copy(
                    message = "Discount application submitted! It will be verified by TODA operators."
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    error = "Failed to apply for discount: ${e.message}"
                )
            }
        }
    }

    fun requestEmergencyAssistance() {
        viewModelScope.launch {
            try {
                val profile = _userProfile.value
                if (profile != null) {
                    repository.createEmergencyAlert(
                        userId = _dashboardState.value.userId ?: "",
                        userName = profile.name,
                        bookingId = null,
                        latitude = 14.74800540601891, // Default to Barangay 177 center
                        longitude = 121.0499004,
                        message = "Emergency assistance requested from customer app"
                    ).fold(
                        onSuccess = { alertId ->
                            _dashboardState.value = _dashboardState.value.copy(
                                message = "Emergency alert sent! Help is on the way. Alert ID: $alertId"
                            )
                        },
                        onFailure = { error ->
                            _dashboardState.value = _dashboardState.value.copy(
                                error = "Failed to send emergency alert: ${error.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    error = "Emergency request failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _dashboardState.value = _dashboardState.value.copy(error = null)
    }

    fun clearMessage() {
        _dashboardState.value = _dashboardState.value.copy(message = null)
    }

}
data class CustomerDashboardState(
    val isLoading: Boolean = false,
    val userId: String? = null,
    val error: String? = null,
    val message: String? = null
)

data class RideSummaryCounts(
    val completed: Int = 0,
    val ongoing: Int = 0,
    val cancelled: Int = 0
)
