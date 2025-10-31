package com.example.toda.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.toda.R

@Composable
fun UserTypeSelection(
    modifier: Modifier = Modifier,
    onUserTypeSelected: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // 👈 set background white
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.moto_logo),
            contentDescription = "App Splash Logo",
            modifier = Modifier.size(200.dp) // adjust size if needed
        )

        Text(
            text = "Welcome to TODA Booking",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Select your portal to login or register",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { onUserTypeSelected("customer") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Passenger Portal")
        }

        Button(
            onClick = { onUserTypeSelected("driver_login") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Driver Portal")
        }

        Button(
            onClick = { onUserTypeSelected("admin") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Administrator Portal")
        }
    }
}
