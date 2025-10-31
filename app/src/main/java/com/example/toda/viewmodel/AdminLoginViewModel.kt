package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.FirebaseUser
import com.example.toda.data.LoginState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    fun login(phoneNumber: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = _loginState.value.copy(isLoading = true, error = null)

                // Create email from phone number for Firebase Auth
                val email = "${phoneNumber}@toda-admin.com"

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    // Fetch admin profile from database
                    val userRef = database.getReference("users/${user.uid}")
                    val snapshot = userRef.get().await()

                    if (snapshot.exists()) {
                        // Use GenericTypeIndicator for proper Firebase data casting
                        val typeIndicator = object : GenericTypeIndicator<Map<String, Any>>() {}
                        val userData = snapshot.getValue(typeIndicator)
                        val userType = userData?.get("userType") as? String

                        if (userType == "ADMIN") {
                            val firebaseUser = FirebaseUser(
                                id = user.uid,
                                name = userData["name"] as? String ?: "",
                                phoneNumber = userData["phoneNumber"] as? String ?: phoneNumber,
                                userType = "ADMIN",
                                // Handle both field naming conventions for backward compatibility
                                isVerified = (userData["isVerified"] as? Boolean)
                                    ?: (userData["verified"] as? Boolean) ?: true,
                                isActive = (userData["isActive"] as? Boolean)
                                    ?: (userData["active"] as? Boolean) ?: true,
                                registrationDate = userData["registrationDate"] as? Long ?: System.currentTimeMillis(),
                                lastActiveTime = userData["lastActiveTime"] as? Long ?: System.currentTimeMillis(),
                                todaId = userData["todaId"] as? String,
                                membershipNumber = userData["membershipNumber"] as? String,
                                membershipStatus = userData["membershipStatus"] as? String ?: "ACTIVE",
                                address = userData["address"] as? String ?: "",
                                emergencyContact = userData["emergencyContact"] as? String ?: "",
                                employeeId = userData["employeeId"] as? String ?: "",
                                position = userData["position"] as? String ?: ""
                            )

                            _currentUser.value = firebaseUser
                            _loginState.value = _loginState.value.copy(
                                isLoading = false,
                                isSuccess = true,
                                userId = user.uid
                            )
                        } else {
                            throw Exception("Access denied: Admin privileges required")
                        }
                    } else {
                        throw Exception("Admin profile not found")
                    }
                } else {
                    throw Exception("Authentication failed")
                }

            } catch (e: Exception) {
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun registerAdmin(
        name: String,
        phoneNumber: String,
        password: String,
        position: String,
        employeeId: String,
        authorizationCode: String,
        address: String = ""
    ) {
        viewModelScope.launch {
            try {
                _loginState.value = _loginState.value.copy(isLoading = true, error = null)

                // Validate authorization code (you can implement your own validation logic)
                if (authorizationCode != "TODA2025ADMIN") {
                    throw Exception("Invalid authorization code")
                }

                // Create email from phone number for Firebase Auth
                val email = "${phoneNumber}@toda-admin.com"

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    // Create admin profile in database
                    val adminData = mapOf(
                        "id" to user.uid,
                        "name" to name,
                        "phoneNumber" to phoneNumber,
                        "userType" to "ADMIN",
                        "position" to position,
                        "employeeId" to employeeId,
                        "address" to address,
                        "emergencyContact" to phoneNumber,
                        "isActive" to true,
                        "isVerified" to true,
                        "registrationDate" to System.currentTimeMillis(),
                        "lastActiveTime" to System.currentTimeMillis(),
                        "membershipStatus" to "ACTIVE",
                        "todaId" to "ADMIN_${System.currentTimeMillis()}",
                        "membershipNumber" to "ADM${System.currentTimeMillis().toString().takeLast(6)}"
                    )

                    // Save to users node
                    database.getReference("users/${user.uid}").setValue(adminData).await()

                    // Save to userProfiles node for consistency
                    val profileData = mapOf(
                        "name" to name,
                        "phoneNumber" to phoneNumber,
                        "userType" to "ADMIN",
                        "position" to position,
                        "employeeId" to employeeId,
                        "address" to address,
                        "emergencyContact" to phoneNumber,
                        "isBlocked" to false,
                        "rating" to 5.0,
                        "trustScore" to 100,
                        "totalBookings" to 0,
                        "completedBookings" to 0,
                        "cancelledBookings" to 0,
                        "earnings" to 0,
                        "totalTrips" to 0,
                        "lastBookingTime" to 0,
                        "yearsOfExperience" to 0
                    )

                    database.getReference("userProfiles/${user.uid}").setValue(profileData).await()

                    val firebaseUser = FirebaseUser(
                        id = user.uid,
                        name = name,
                        phoneNumber = phoneNumber,
                        userType = "ADMIN",
                        isVerified = true,
                        isActive = true,
                        registrationDate = System.currentTimeMillis(),
                        lastActiveTime = System.currentTimeMillis(),
                        todaId = "ADMIN_${System.currentTimeMillis()}",
                        membershipNumber = "ADM${System.currentTimeMillis().toString().takeLast(6)}",
                        membershipStatus = "ACTIVE",
                        address = address,
                        emergencyContact = phoneNumber,
                        employeeId = employeeId,
                        position = position
                    )

                    _currentUser.value = firebaseUser
                    _loginState.value = _loginState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        userId = user.uid
                    )
                } else {
                    throw Exception("Registration failed")
                }

            } catch (e: Exception) {
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registration failed"
                )
            }
        }
    }

    fun clearError() {
        _loginState.value = _loginState.value.copy(error = null)
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _loginState.value = LoginState()
    }
}
