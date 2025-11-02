package com.example.toda.ui.barker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.toda.data.Booking
import com.example.toda.data.User
import com.example.toda.service.GeocodingService
import com.example.toda.ui.components.OSMMapView
import com.example.toda.utils.FeeCalculator
import com.example.toda.viewmodel.EnhancedBookingViewModel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarkerInterface(
    onLogout: () -> Unit,
    viewModel: EnhancedBookingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val geocodingService = remember { GeocodingService() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var pickupLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var pickupGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var dropoffGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var isSelectingPickup by remember { mutableStateOf(false) }
    var isSelectingDropoff by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var estimatedFare by remember { mutableStateOf(0.0) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Calculate fare when both locations are selected
    LaunchedEffect(pickupGeoPoint, dropoffGeoPoint) {
        if (pickupGeoPoint != null && dropoffGeoPoint != null) {
            val driverLocation = GeoPoint(14.746, 121.048) // Default TODA location
            // Use the same fare calculation logic as CustomerInterface
            val passengerDistance = calculateDistance(pickupGeoPoint!!, dropoffGeoPoint!!)
            val driverToPickupDistance = calculateDistance(driverLocation, pickupGeoPoint!!)

            // Base fare calculation
            val baseFare = calculateFare(passengerDistance)

            // Driver travel fee
            val driverTravelFee = if (driverToPickupDistance <= 1.0) {
                0.0
            } else {
                (driverToPickupDistance - 1.0) * 5.0
            }

            // Convenience fee (no discount for walk-in customers)
            val convenienceFee = FeeCalculator.convenienceFee(null)

            // Total fare
            estimatedFare = baseFare + driverTravelFee + convenienceFee
        } else {
            estimatedFare = 0.0
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
            // Customer Information Card
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
                        text = "Customer Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        placeholder = { Text("Enter full name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = {
                            // Only allow numbers and limit to 11 digits
                            if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                                customerPhone = it
                            }
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("09123456789") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Location Selection Card
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
                        text = "Trip Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    // Pickup Location
                    OutlinedTextField(
                        value = pickupLocation,
                        onValueChange = { },
                        label = { Text("Pickup Location") },
                        placeholder = { Text("Tap map to select") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                        },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { isSelectingPickup = true }) {
                                Text("Select on Map")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        enabled = false
                    )

                    // Destination
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { },
                        label = { Text("Destination") },
                        placeholder = { Text("Tap map to select") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F)
                            )
                        },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { isSelectingDropoff = true }) {
                                Text("Select on Map")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        enabled = false
                    )

                    // Map selection status
                    if (isSelectingPickup || isSelectingDropoff) {
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSelectingPickup)
                                        "Tap the map to select pickup location"
                                    else
                                        "Tap the map to select destination",
                                    fontSize = 14.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Estimated Fare Display
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
                            }
                        }
                    }
                }
            }

            // Map View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OSMMapView(
                        modifier = Modifier.fillMaxSize(),
                        pickupLocation = pickupGeoPoint,
                        dropoffLocation = dropoffGeoPoint,
                        onMapClick = { geoPoint ->
                            when {
                                isSelectingPickup -> {
                                    pickupGeoPoint = geoPoint
                                    coroutineScope.launch {
                                        isLoadingLocation = true
                                        val locationName = geocodingService.reverseGeocode(
                                            geoPoint.latitude,
                                            geoPoint.longitude
                                        ) ?: "Selected Location"
                                        pickupLocation = locationName
                                        isLoadingLocation = false
                                        isSelectingPickup = false
                                    }
                                }
                                isSelectingDropoff -> {
                                    dropoffGeoPoint = geoPoint
                                    coroutineScope.launch {
                                        isLoadingLocation = true
                                        val locationName = geocodingService.reverseGeocode(
                                            geoPoint.latitude,
                                            geoPoint.longitude
                                        ) ?: "Selected Location"
                                        destination = locationName
                                        isLoadingLocation = false
                                        isSelectingDropoff = false
                                    }
                                }
                            }
                        }
                    )

                    // Loading overlay
                    if (isLoadingLocation) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    // Validate form
                    when {
                        customerName.isBlank() -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please enter customer name")
                            }
                        }
                        customerPhone.length != 11 -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please enter valid 11-digit phone number")
                            }
                        }
                        pickupGeoPoint == null -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please select pickup location")
                            }
                        }
                        dropoffGeoPoint == null -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please select destination")
                            }
                        }
                        else -> {
                            // Submit booking
                            isSubmitting = true

                            // Use ViewModel's createBooking method
                            viewModel.createBooking(
                                customerId = "walkin_${System.currentTimeMillis()}",
                                customerName = customerName,
                                phoneNumber = customerPhone,
                                pickupLocation = pickupLocation,
                                destination = destination,
                                pickupGeoPoint = pickupGeoPoint!!,
                                dropoffGeoPoint = dropoffGeoPoint!!,
                                estimatedFare = estimatedFare
                            )

                            // Clear form and show success
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000) // Wait for submission

                                customerName = ""
                                customerPhone = ""
                                pickupLocation = ""
                                destination = ""
                                pickupGeoPoint = null
                                dropoffGeoPoint = null
                                estimatedFare = 0.0

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

// Generate a 4-digit verification code
private fun generateVerificationCode(): String {
    return (1000..9999).random().toString()
}

// Calculate distance between two GeoPoints in kilometers
private fun calculateDistance(from: GeoPoint, to: GeoPoint): Double {
    val earthRadius = 6371.0 // Earth radius in kilometers
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(from.latitude)) *
            kotlin.math.cos(Math.toRadians(to.latitude)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadius * c
}

// Calculate fare based on distance
private fun calculateFare(distance: Double): Double {
    // Base fare structure from CustomerInterface
    return when {
        distance <= 1.0 -> 15.0 // ₱15 for first km
        distance <= 2.0 -> 15.0 + ((distance - 1.0) * 5.0) // +₱5/km for 2nd km
        else -> 20.0 + ((distance - 2.0) * 10.0) // ₱20 base + ₱10/km after 2km
    }
}
