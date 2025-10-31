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
class CustomerRegistrationViewModel @Inject constructor(
    private val repository: TODARepository
) : ViewModel() {

    private val _registrationState = MutableStateFlow(CustomerRegistrationState())
    val registrationState = _registrationState.asStateFlow()

    private val _validationErrors = MutableStateFlow(ValidationErrors())
    val validationErrors = _validationErrors.asStateFlow()

    fun validateStep1(
        name: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        val errors = ValidationErrors()

        if (name.isBlank()) {
            errors.nameError = "Name is required"
        }

        if (phoneNumber.isBlank()) {
            errors.phoneError = "Phone number is required"
        } else if (!isValidPhoneNumber(phoneNumber)) {
            errors.phoneError = "Phone number must be 11 digits starting with 09 (e.g., 09XXXXXXXXX)"
        }

        if (password.isBlank()) {
            errors.passwordError = "Password is required"
        } else if (password.length < 8) {
            errors.passwordError = "Password must be at least 8 characters"
        }

        if (confirmPassword != password) {
            errors.confirmPasswordError = "Passwords do not match"
        }

        _validationErrors.value = errors
        return !errors.hasErrors()
    }

    fun validateStep2(
        address: String,
        dateOfBirth: Long?,
        gender: String,
        occupation: String
    ): Boolean {
        val errors = ValidationErrors()

        if (address.isBlank()) {
            errors.addressError = "Address is required"
        }

        if (dateOfBirth == null) {
            errors.dobError = "Date of birth is required"
        } else {
            val age = calculateAge(dateOfBirth)
            if (age < 18) {
                errors.dobError = "Must be at least 18 years old"
            }
        }

        if (gender.isBlank()) {
            errors.genderError = "Gender is required"
        }

        _validationErrors.value = errors
        return !errors.hasErrors()
    }

    fun registerCustomer(
        name: String,
        phoneNumber: String,
        password: String,
        address: String,
        dateOfBirth: Long?,
        gender: String,
        occupation: String,
        notificationPreferences: NotificationPreferences,
        agreesToTerms: Boolean
    ) {
        if (!agreesToTerms) {
            _registrationState.value = _registrationState.value.copy(
                error = "You must agree to the terms and conditions"
            )
            return
        }

        viewModelScope.launch {
            try {
                _registrationState.value = _registrationState.value.copy(
                    isRegistering = true,
                    error = null
                )

                // Register user in Firebase Auth system
                val userResult = repository.registerUser(
                    phoneNumber = phoneNumber,
                    name = name,
                    userType = UserType.PASSENGER,
                    password = password
                )

                userResult.fold(
                    onSuccess = { userId ->
                        // Create detailed user profile
                        val profile = UserProfile(
                            phoneNumber = phoneNumber,
                            name = name,
                            userType = UserType.PASSENGER,
                            address = address,
                            emergencyContact = "", // Remove emergency contact requirement
                            totalBookings = 0,
                            completedBookings = 0,
                            cancelledBookings = 0,
                            trustScore = 100.0,
                            isBlocked = false
                        )

                        // Save user profile
                        repository.updateUserProfile(userId, profile).fold(
                            onSuccess = {
                                // Create passenger registration record
                                val passengerRegistration = PassengerRegistration(
                                    id = userId,
                                    name = name,
                                    phoneNumber = phoneNumber,
                                    address = address,
                                    emergencyContact = "", // Remove emergency contact requirement
                                    emergencyContactName = "", // Remove emergency contact requirement
                                    dateOfBirth = dateOfBirth,
                                    gender = gender,
                                    occupation = occupation,
                                    registrationDate = System.currentTimeMillis(),
                                    isPhoneVerified = true, // Will implement SMS verification later
                                    agreesToTerms = agreesToTerms,
                                    notificationPreferences = notificationPreferences
                                )

                                _registrationState.value = _registrationState.value.copy(
                                    isRegistering = false,
                                    isSuccess = true,
                                    userId = userId,
                                    message = "Registration successful! Welcome to TODA Booking System."
                                )
                            },
                            onFailure = { error ->
                                _registrationState.value = _registrationState.value.copy(
                                    isRegistering = false,
                                    error = "Failed to create profile: ${error.message}"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _registrationState.value = _registrationState.value.copy(
                            isRegistering = false,
                            error = "Registration failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _registrationState.value = _registrationState.value.copy(
                    isRegistering = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _registrationState.value = _registrationState.value.copy(error = null)
    }

    fun clearValidationErrors() {
        _validationErrors.value = ValidationErrors()
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Philippine phone number validation - only allow 11 digits starting with "09"
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        return cleanNumber.startsWith("09") && cleanNumber.length == 11
    }

    private fun calculateAge(birthDate: Long): Int {
        val birth = java.util.Calendar.getInstance().apply { timeInMillis = birthDate }
        val today = java.util.Calendar.getInstance()

        var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)

        if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }
}

data class CustomerRegistrationState(
    val isRegistering: Boolean = false,
    val isCheckingPhone: Boolean = false,
    val isSuccess: Boolean = false,
    val userId: String? = null,
    val error: String? = null,
    val message: String? = null
)

data class ValidationErrors(
    var nameError: String? = null,
    var phoneError: String? = null,
    var passwordError: String? = null,
    var confirmPasswordError: String? = null,
    var addressError: String? = null,
    var dobError: String? = null,
    var genderError: String? = null,
    var occupationError: String? = null,
    var emergencyNameError: String? = null,
    var emergencyContactError: String? = null
) {
    fun hasErrors(): Boolean = listOf(
        nameError, phoneError, passwordError, confirmPasswordError,
        addressError, dobError, genderError, occupationError,
        emergencyNameError, emergencyContactError
    ).any { it != null }

    fun toMap(): Map<String, String> {
        val errorMap = mutableMapOf<String, String>()
        nameError?.let { errorMap["name"] = it }
        phoneError?.let { errorMap["phoneNumber"] = it }
        passwordError?.let { errorMap["password"] = it }
        confirmPasswordError?.let { errorMap["confirmPassword"] = it }
        addressError?.let { errorMap["address"] = it }
        dobError?.let { errorMap["dateOfBirth"] = it }
        genderError?.let { errorMap["gender"] = it }
        occupationError?.let { errorMap["occupation"] = it }
        emergencyNameError?.let { errorMap["emergencyContactName"] = it }
        emergencyContactError?.let { errorMap["emergencyContact"] = it }
        return errorMap
    }
}
