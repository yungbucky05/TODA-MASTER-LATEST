package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.*
import com.example.toda.repository.TODARepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import android.util.Patterns

@HiltViewModel
class CustomerLoginViewModel @Inject constructor(
    private val repository: TODARepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _loginState = MutableStateFlow(CustomerLoginState())
    val loginState: StateFlow<CustomerLoginState> = _loginState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // New: password reset state
    private val _passwordReset: MutableStateFlow<PasswordResetState> = MutableStateFlow(PasswordResetState())
    val passwordReset: StateFlow<PasswordResetState> = _passwordReset.asStateFlow()

    fun login(phoneNumber: String, password: String) {
        if (phoneNumber.isBlank() || password.isBlank()) {
            _loginState.value = _loginState.value.copy(
                error = "Please enter both phone number and password"
            )
            return
        }

        // Validate phone number format
        if (!isValidPhoneNumber(phoneNumber)) {
            _loginState.value = _loginState.value.copy(
                error = "Please enter a valid Philippine phone number (e.g., 09XXXXXXXXX)"
            )
            return
        }

        viewModelScope.launch {
            try {
                _loginState.value = _loginState.value.copy(
                    isLoading = true,
                    error = null
                )

                println("=== CUSTOMER LOGIN ATTEMPT ===")
                println("Phone: $phoneNumber")

                repository.loginUser(phoneNumber, password).fold(
                    onSuccess = { user ->
                        println("Login successful for user: ${user.name} (${user.userType})")

                        if (user.userType == "PASSENGER") {
                            _currentUser.value = user
                            _loginState.value = _loginState.value.copy(
                                isLoading = false,
                                isSuccess = true,
                                userId = user.id
                            )
                            println("Customer login successful")
                        } else {
                            _loginState.value = _loginState.value.copy(
                                isLoading = false,
                                error = "This account is registered as ${user.userType}. Please use the appropriate app."
                            )
                            println("Wrong user type: ${user.userType}")
                        }
                    },
                    onFailure = { error ->
                        println("Login failed: ${error.message}")

                        // Provide more specific error messages
                        val errorMessage = when {
                            error.message?.contains("authentication failed", ignoreCase = true) == true ->
                                "Invalid phone number or password. Please check your credentials."
                            error.message?.contains("phone number already registered", ignoreCase = true) == true ->
                                "Phone number not found. Please register first or check your number."
                            error.message?.contains("network error", ignoreCase = true) == true ||
                            error.message?.contains("timeout", ignoreCase = true) == true ||
                            error.message?.contains("interrupted connection", ignoreCase = true) == true ||
                            error.message?.contains("unreachable host", ignoreCase = true) == true ||
                            error.message?.contains("unable to resolve host", ignoreCase = true) == true ||
                            error.message?.contains("no address associated", ignoreCase = true) == true ->
                                "Network connection problem detected. Please check your internet connection and try again. Make sure you have a stable WiFi or mobile data connection."
                            else -> "Login failed: ${error.message}"
                        }

                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                println("Login exception: ${e.message}")
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    error = "Login failed: ${e.message ?: "Unknown error occurred"}"
                )
            }
        }
    }

    fun register(
        name: String,
        phoneNumber: String,
        email: String,
        password: String,
        address: String,
        dateOfBirth: Long?,
        gender: String,
        occupation: String,
        notificationPreferences: Map<String, Boolean>
    ) {
        if (phoneNumber.isBlank() || password.isBlank() || name.isBlank() || email.isBlank()) {
            _loginState.value = _loginState.value.copy(
                error = "Please enter all required fields"
            )
            return
        }

        // Validate phone number format
        if (!isValidPhoneNumber(phoneNumber)) {
            _loginState.value = _loginState.value.copy(
                error = "Please enter a valid Philippine phone number (e.g., 09XXXXXXXXX)"
            )
            return
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.value = _loginState.value.copy(
                error = "Please enter a valid email address"
            )
            return
        }

        viewModelScope.launch {
            try {
                _loginState.value = _loginState.value.copy(
                    isLoading = true,
                    error = null
                )

                println("=== CUSTOMER REGISTRATION ATTEMPT ===")
                println("Name: $name")
                println("Phone: $phoneNumber")
                println("Email: $email")

                val userData = mapOf(
                    "name" to name,
                    "phoneNumber" to phoneNumber,
                    "email" to email,
                    "address" to address,
                    "dateOfBirth" to (dateOfBirth ?: 0),
                    "gender" to gender,
                    "occupation" to occupation,
                    "active" to true,
                    "userType" to "PASSENGER",
                    "verified" to true,
                    "membershipStatus" to "ACTIVE",
                    "registrationDate" to Date().time,
                    "lastActiveTime" to Date().time,
                    "notificationPreferences" to notificationPreferences
                )

                repository.registerUser(email, phoneNumber, password, userData).fold(
                    onSuccess = { user ->
                        println("Registration successful for user: ${user.name}")

                        _currentUser.value = user
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            userId = user.id
                        )
                    },
                    onFailure = { error ->
                        println("Registration failed: ${error.message}")

                        // Provide specific error messages
                        val errorMessage = when {
                            error.message?.contains("already in use", ignoreCase = true) == true ->
                                "This phone number is already registered. Please login or use another number."
                            error.message?.contains("network error", ignoreCase = true) == true ||
                            error.message?.contains("timeout", ignoreCase = true) == true ||
                            error.message?.contains("interrupted connection", ignoreCase = true) == true ||
                            error.message?.contains("unreachable host", ignoreCase = true) == true ||
                            error.message?.contains("unable to resolve host", ignoreCase = true) == true ||
                            error.message?.contains("no address associated", ignoreCase = true) == true ->
                                "Network connection problem detected. Please check your internet connection and try again. Make sure you have a stable WiFi or mobile data connection."
                            else -> "Registration failed: ${error.message}"
                        }

                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                println("Registration exception: ${e.message}")
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    error = "Registration failed: ${e.message ?: "Unknown error occurred"}"
                )
            }
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Philippine phone number validation - only allow 11 digits starting with "09"
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        return cleanNumber.startsWith("09") && cleanNumber.length == 11
    }

    fun clearError() {
        _loginState.value = _loginState.value.copy(error = null)
    }

    fun logout() {
        // Sign out from Firebase Authentication
        auth.signOut()

        // Clear local state
        _currentUser.value = null
        _loginState.value = CustomerLoginState()
    }

    // New: trigger Firebase password reset email (requires the account email)
    fun resetPassword(email: String) {
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _passwordReset.value = PasswordResetState(error = "Enter a valid email address")
            return
        }
        viewModelScope.launch {
            _passwordReset.value = PasswordResetState(isSending = true)
            val result = repository.sendPasswordResetEmail(email)
            _passwordReset.value = result.fold(
                onSuccess = { PasswordResetState(sent = true) },
                onFailure = { PasswordResetState(error = it.message ?: "Failed to send reset email") }
            )
        }
    }

    fun clearPasswordReset() { _passwordReset.value = PasswordResetState() }
}

data class CustomerLoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val userId: String? = null,
    val error: String? = null
)

// New state holder for password reset UI
data class PasswordResetState(
    val isSending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null
)
