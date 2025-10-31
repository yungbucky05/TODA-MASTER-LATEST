package com.example.toda.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.toda.data.*
import com.example.toda.service.RegistrationService
import com.example.toda.service.RegistrationResult
import com.example.toda.ui.registration.PassengerRegistrationScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    onAuthSuccess: (User) -> Unit,
    onBack: () -> Unit
) {
    // Simplified to only handle passenger registration
    // since registration is now integrated into each login screen
    PassengerRegistrationScreen(
        onRegistrationComplete = { userId ->
            // Registration successful - create user object and call success
            val user = User(
                id = userId,
                phoneNumber = "", // Will be filled by the registration screen
                name = "", // Will be filled by the registration screen
                userType = UserType.PASSENGER,
                isVerified = true,
                password = "" // Not needed for successful registration
            )
            onAuthSuccess(user)
        },
        onBack = onBack
    )
}