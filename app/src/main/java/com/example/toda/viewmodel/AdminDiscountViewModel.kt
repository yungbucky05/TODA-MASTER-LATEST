package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.DiscountType
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class DiscountApplication(
    val userId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val discountType: String = "",
    val discountIdNumber: String = "",
    val discountIdImageUrl: String = "",
    val discountVerified: Boolean = false
)

@HiltViewModel
class AdminDiscountViewModel @Inject constructor() : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    private val _pendingApplications = MutableStateFlow<List<Pair<String, DiscountApplication>>>(emptyList())
    val pendingApplications: StateFlow<List<Pair<String, DiscountApplication>>> = _pendingApplications.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPendingApplications()
        startListeningForPendingCount()
    }

    fun loadPendingApplications() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            _error.value = null

            try {
                val snapshot = usersRef.get().await()
                val applications = mutableListOf<Pair<String, DiscountApplication>>()

                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    val userData = userSnapshot.value as? Map<*, *> ?: return@forEach

                    // Check if user has a discount type set but not yet verified
                    // Handle both String and Map (enum object) formats
                    val discountTypeValue = userData["discountType"]
                    val discountType = when (discountTypeValue) {
                        is String -> discountTypeValue
                        is Map<*, *> -> {
                            // If stored as enum object, extract the enum name from displayName
                            val displayName = discountTypeValue["displayName"] as? String
                            when (displayName) {
                                "Person with Disability" -> "PWD"
                                "Senior Citizen" -> "SENIOR_CITIZEN"
                                "Student" -> "STUDENT"
                                else -> null
                            }
                        }
                        else -> null
                    }

                    val discountVerified = userData["discountVerified"] as? Boolean ?: false
                    val discountIdImageUrl = userData["discountIdImageUrl"] as? String ?: ""

                    if (discountType != null && !discountVerified && discountIdImageUrl.isNotEmpty()) {
                        val application = DiscountApplication(
                            userId = userId,
                            name = userData["name"] as? String ?: "",
                            phoneNumber = userData["phoneNumber"] as? String ?: "",
                            discountType = discountType,
                            discountIdNumber = userData["discountIdNumber"] as? String ?: "",
                            discountIdImageUrl = discountIdImageUrl,
                            discountVerified = discountVerified
                        )
                        applications.add(Pair(userId, application))
                    }
                }

                _pendingApplications.value = applications
                _pendingCount.value = applications.size
            } catch (e: Exception) {
                _error.value = "Failed to load applications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startListeningForPendingCount() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                snapshot.children.forEach { userSnapshot ->
                    val userData = userSnapshot.value as? Map<*, *> ?: return@forEach

                    // Handle both String and Map (enum object) formats
                    val discountTypeValue = userData["discountType"]
                    val discountType = when (discountTypeValue) {
                        is String -> discountTypeValue
                        is Map<*, *> -> {
                            val displayName = discountTypeValue["displayName"] as? String
                            when (displayName) {
                                "Person with Disability" -> "PWD"
                                "Senior Citizen" -> "SENIOR_CITIZEN"
                                "Student" -> "STUDENT"
                                else -> null
                            }
                        }
                        else -> null
                    }

                    val discountVerified = userData["discountVerified"] as? Boolean ?: false
                    val discountIdImageUrl = userData["discountIdImageUrl"] as? String ?: ""

                    if (discountType != null && !discountVerified && discountIdImageUrl.isNotEmpty()) {
                        count++
                    }
                }
                _pendingCount.value = count
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun approveDiscount(userId: String, application: DiscountApplication) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            _error.value = null

            try {
                // Set discount as verified with expiry date (1 year from now)
                val expiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)

                val updates = mapOf(
                    "discountVerified" to true,
                    "discountExpiryDate" to expiryDate
                )

                usersRef.child(userId).updateChildren(updates).await()

                _message.value = "Discount approved for ${application.name}"
                loadPendingApplications() // Reload the list
            } catch (e: Exception) {
                _error.value = "Failed to approve discount: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectDiscount(userId: String, application: DiscountApplication) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            _error.value = null

            try {
                // Clear discount information
                val updates = mapOf(
                    "discountType" to null,
                    "discountIdNumber" to "",
                    "discountIdImageUrl" to "",
                    "discountVerified" to false,
                    "discountExpiryDate" to null
                )

                usersRef.child(userId).updateChildren(updates).await()

                _message.value = "Discount rejected for ${application.name}"
                loadPendingApplications() // Reload the list
            } catch (e: Exception) {
                _error.value = "Failed to reject discount: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
        _error.value = null
    }
}
