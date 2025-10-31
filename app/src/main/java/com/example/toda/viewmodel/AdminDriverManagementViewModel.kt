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

data class AdminDriverManagementUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AdminDriverManagementViewModel @Inject constructor(
    private val repository: TODARepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDriverManagementUiState())
    val uiState: StateFlow<AdminDriverManagementUiState> = _uiState.asStateFlow()

    private val _allDrivers = MutableStateFlow<List<Driver>>(emptyList())
    val allDrivers: StateFlow<List<Driver>> = _allDrivers.asStateFlow()

    fun loadAllDrivers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                println("=== LOADING ALL DRIVERS ===")
                val result = repository.getAllDrivers()

                result.fold(
                    onSuccess = { drivers ->
                        println("Received ${drivers.size} drivers")
                        drivers.forEach { driver ->
                            println("Driver: ${driver.id} - ${driver.name} - RFID: ${driver.rfidUID} - Has RFID: ${driver.hasRfidAssigned}")
                        }
                        _allDrivers.value = drivers
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    },
                    onFailure = { exception ->
                        println("Error loading drivers: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message
                        )
                    }
                )
            } catch (e: Exception) {
                println("Exception loading drivers: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun assignRfid(driverId: String, rfidUID: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = repository.assignRfidToDriver(driverId, rfidUID)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "RFID assigned successfully!"
                        )
                        loadAllDrivers() // Refresh the list
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to assign RFID: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error assigning RFID: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
