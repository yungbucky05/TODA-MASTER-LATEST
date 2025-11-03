package com.example.toda.ui.barker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.data.Booking
import com.example.toda.data.BookingStatus
import com.example.toda.viewmodel.EnhancedBookingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarkerInterface(
    onLogout: () -> Unit,
    viewModel: EnhancedBookingViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Observe active bookings to detect driver assignment
    val activeBookings by viewModel.activeBookings.collectAsStateWithLifecycle()
    
    // Observe fare matrix from database
    val fareMatrix by viewModel.regularFareMatrix.collectAsStateWithLifecycle()
    
    // Track the last submitted booking and driver info
    var lastSubmittedBookingId by remember { mutableStateOf<String?>(null) }
    var lastDriverInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) } // Name, TODA, RFID

    val terminalGeoPoint = remember {
        GeoPoint(14.746010978688304, 121.0513973236084)
    }
    val pickupLocationLabel = "TODA Barker Terminal"

    val destinationOptions = remember {
        listOf(
            DestinationOption("Cielito", null),
            DestinationOption("Cristina", null),
            DestinationOption("Castlespring Heights", null)
        )
    } // TODO: Update destination coordinates when they are finalized.

    var selectedDestination by remember { mutableStateOf<DestinationOption?>(null) }
    var dropoffGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var passengerCount by remember { mutableStateOf(3) }
    var estimatedFare by remember { mutableStateOf(0.0) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Calculate fare: (baseFare * 3) / number of passengers
    LaunchedEffect(passengerCount, selectedDestination, fareMatrix) {
        estimatedFare = if (selectedDestination != null) {
            (fareMatrix.baseFare * 3) / passengerCount
        } else {
            0.0
        }
    }
    
    // Watch for driver assignment on the last submitted booking
    LaunchedEffect(lastSubmittedBookingId, activeBookings) {
        val bookingId = lastSubmittedBookingId
        if (bookingId != null) {
            val booking = activeBookings.find { it.id == bookingId }
            if (booking != null && booking.status == BookingStatus.ACCEPTED && 
                booking.driverName.isNotBlank()) {
                // Driver assigned! Show the info
                lastDriverInfo = Triple(
                    booking.driverName,
                    booking.todaNumber.ifBlank { "N/A" },
                    booking.driverRFID.ifBlank { "N/A" }
                )
                // Auto-clear after 10 seconds
                delay(10000)
                lastDriverInfo = null
                lastSubmittedBookingId = null
            }
        }
    }
    
    // Listen for booking state to capture the booking ID
    val bookingState by viewModel.bookingState.collectAsStateWithLifecycle()
    LaunchedEffect(bookingState.currentBookingId) {
        bookingState.currentBookingId?.let { bookingId ->
            // This is our new booking - watch for driver assignment
            lastSubmittedBookingId = bookingId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TODA Barker Terminal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Walk-in Customer Booking",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pickup Point",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    Text(
                        text = pickupLocationLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select Destination",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    destinationOptions.forEach { option ->
                        val isSelected = selectedDestination?.label == option.label
                        Card(
                            onClick = {
                                selectedDestination = option
                                dropoffGeoPoint = option.location ?: terminalGeoPoint
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1976D2)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1976D2) else Color.White,
                                contentColor = if (isSelected) Color.White else Color(0xFF1976D2)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Passengers",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { if (passengerCount > 1) passengerCount -= 1 },
                                enabled = passengerCount > 1,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "-")
                            }

                            Text(
                                text = passengerCount.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            OutlinedButton(
                                onClick = { if (passengerCount < 3) passengerCount += 1 },
                                enabled = passengerCount < 3,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "+")
                            }
                        }
                    }

                    if (estimatedFare > 0) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Estimated Fare",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "₱${String.format(Locale.US, "%.2f", estimatedFare)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "per passenger",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    selectedDestination?.let { option ->
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Selected Destination",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = option.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
            // Submit Button
            Button(
                onClick = {
                    when {
                        selectedDestination == null -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please choose a destination")
                            }
                        }
                        else -> {
                            isSubmitting = true

                            viewModel.createBooking(
                                customerId = "walkin_${System.currentTimeMillis()}",
                                customerName = "Walk-in Customer",
                                phoneNumber = "00000000000",
                                pickupLocation = pickupLocationLabel,
                                destination = selectedDestination!!.label,
                                pickupGeoPoint = terminalGeoPoint,
                                dropoffGeoPoint = dropoffGeoPoint!!,
                                estimatedFare = estimatedFare
                            )

                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000) // Wait for submission

                                selectedDestination = null
                                dropoffGeoPoint = null
                                estimatedFare = 0.0
                                passengerCount = 3

                                snackbarHostState.showSnackbar(
                                    "Booking submitted successfully!",
                                    duration = SnackbarDuration.Long
                                )
                                isSubmitting = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Booking",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Driver info card (shown when driver is assigned)
            lastDriverInfo?.let { (name, toda, rfid) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Driver Assigned!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Driver Name: $name",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "TODA Number: $toda",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "RFID: $rfid",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        Text(
                            text = "Driver info will disappear in a few seconds...",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Walk-in bookings are automatically assigned to the next available driver in queue.",
                        fontSize = 12.sp,
                        color = Color(0xFF795548)
                    )
                }
            }
        }
    }
}

private data class DestinationOption(
    val label: String,
    val location: GeoPoint?
)
