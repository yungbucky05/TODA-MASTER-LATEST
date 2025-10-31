package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.Driver
import com.example.toda.repository.TODARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverRegistrationUiState(
    val isLoading: Boolean = false,
    val isRegistrationSuccessful: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DriverRegistrationViewModel @Inject constructor(
    private val repository: TODARepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverRegistrationUiState())
    val uiState: StateFlow<DriverRegistrationUiState> = _uiState.asStateFlow()

    fun submitRegistration(driver: Driver) {
        // Validate Philippine Driver's License
        if (!isValidPhilippineLicense(driver.licenseNumber)) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please enter a valid Philippine Driver's License number (e.g., N12-34-567890)"
            )
            return
        }
        // Validate Tricycle Plate Number
        if (!isValidTricyclePlate(driver.tricyclePlateNumber)) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please enter a valid Tricycle Plate Number (e.g., ABC-1234)"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val result = repository.submitDriverApplication(driver)

                result.fold(
                    onSuccess = { driverId ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRegistrationSuccessful = true
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            isRegistrationSuccessful = false
        )
    }

    fun setValidationError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = errorMessage
        )
    }

    // Validation for Philippine Driver's License (common format: N12-34-567890)
    private fun isValidPhilippineLicense(license: String): Boolean {
        val regex = Regex("^[A-Z]{1,2}[0-9]{2}-[0-9]{2}-[0-9]{6}")
        return regex.matches(license.trim())
    }

    // Validation for Tricycle Plate Number (common format: ABC-1234)
    private fun isValidTricyclePlate(plate: String): Boolean {
        val regex = Regex("^[A-Z]{3}-[0-9]{4}")
        return regex.matches(plate.trim())
    }
}
