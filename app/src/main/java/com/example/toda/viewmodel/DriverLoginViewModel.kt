package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.*
import com.example.toda.repository.TODARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DriverLoginViewModel @Inject constructor(
    private val repository: TODARepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(DriverLoginState())
    val loginState = _loginState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser = _currentUser.asStateFlow()

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

                println("=== DRIVER LOGIN ATTEMPT ===")
                println("Phone: $phoneNumber")

                // Check driver application status first
                repository.getDriverApplicationStatus(phoneNumber).fold(
                    onSuccess = { applicationStatus ->
                        println("Driver application status: $applicationStatus")

                        when (applicationStatus) {
                            "APPROVED" -> {
                                // Driver is approved, proceed with login
                                viewModelScope.launch {
                                    repository.loginUser(phoneNumber, password).fold(
                                        onSuccess = { user ->
                                            println("Login successful - User: ${user.name}, Type: ${user.userType}")

                                            if (user.userType == "DRIVER") {
                                                println("Login approved - Driver with APPROVED status")
                                                _currentUser.value = user
                                                _loginState.value = _loginState.value.copy(
                                                    isLoading = false,
                                                    isSuccess = true,
                                                    userId = user.id
                                                )
                                            } else {
                                                println("Login rejected - Not a driver account")
                                                _loginState.value = _loginState.value.copy(
                                                    isLoading = false,
                                                    error = "This account is not registered as a driver. Please use the correct login or register as a driver."
                                                )
                                            }
                                        },
                                        onFailure = { error ->
                                            println("Login failed: ${error.message}")
                                            _loginState.value = _loginState.value.copy(
                                                isLoading = false,
                                                error = error.message ?: "Login failed. Please check your credentials."
                                            )
                                        }
                                    )
                                }
                            }
                            "PENDING" -> {
                                println("Login rejected - Application still pending")
                                _loginState.value = _loginState.value.copy(
                                    isLoading = false,
                                    error = "Your driver application is still pending approval by the TODA admin. Please wait for approval or contact the admin office."
                                )
                            }
                            "REJECTED" -> {
                                println("Login rejected - Application was rejected")
                                _loginState.value = _loginState.value.copy(
                                    isLoading = false,
                                    error = "Your driver application has been rejected. Please contact the TODA admin office for more information."
                                )
                            }
                            "UNDER_REVIEW" -> {
                                println("Login rejected - Application under review")
                                _loginState.value = _loginState.value.copy(
                                    isLoading = false,
                                    error = "Your driver application is currently under review. Please wait for the review process to complete."
                                )
                            }
                            else -> {
                                println("Login rejected - Unknown application status: $applicationStatus")
                                _loginState.value = _loginState.value.copy(
                                    isLoading = false,
                                    error = "Unable to verify driver status. Please contact the TODA admin office."
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        println("Failed to check driver application status: ${error.message}")

                        // If application status check fails, try direct login (fallback for existing drivers)
                        println("Attempting direct login fallback...")
                        viewModelScope.launch {
                            repository.loginUser(phoneNumber, password).fold(
                                onSuccess = { user ->
                                    println("Direct login successful - User: ${user.name}, Type: ${user.userType}")

                                    if (user.userType == "DRIVER") {
                                        println("Direct login approved - Driver account found")
                                        _currentUser.value = user
                                        _loginState.value = _loginState.value.copy(
                                            isLoading = false,
                                            isSuccess = true,
                                            userId = user.id
                                        )
                                    } else {
                                        println("Direct login rejected - Not a driver account")
                                        _loginState.value = _loginState.value.copy(
                                            isLoading = false,
                                            error = "This account is not registered as a driver. Please register as a driver first."
                                        )
                                    }
                                },
                                onFailure = { loginError ->
                                    println("Direct login also failed: ${loginError.message}")
                                    _loginState.value = _loginState.value.copy(
                                        isLoading = false,
                                        error = "Login failed. Please check your phone number and password, or contact the TODA admin if you're a registered driver."
                                    )
                                }
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                println("Login exception: ${e.message}")
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    error = "An error occurred during login. Please try again."
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
        _currentUser.value = null
        _loginState.value = DriverLoginState()
    }
}

data class DriverLoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val userId: String? = null
)
