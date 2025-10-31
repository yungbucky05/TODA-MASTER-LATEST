package com.example.toda.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.toda.data.*
import com.example.toda.ui.chat.ChatFloatingActionButton
import com.example.toda.ui.components.OSMMapView
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.toda.ui.chat.SimpleChatScreen
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.toda.viewmodel.EnhancedBookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveBookingScreen(
    booking: Booking,
    currentUser: User,
    onBack: () -> Unit,
    onNavigateToFullChat: () -> Unit,
    onCancelBooking: (String) -> Unit, // NEW
    modifier: Modifier = Modifier,
    driver: User? = null,
    operator: User? = null,
    onLocationShare: () -> Unit = {}
) {
    val context = LocalContext.current
    val bookingViewModel: EnhancedBookingViewModel = hiltViewModel()

    // Show timeout dialog after 2 minutes if still pending
    var showNoDriversDialog by remember(booking.id) { mutableStateOf(false) }
    // Track if auto-cancellation was already triggered to prevent duplicate calls
    var autoCancelled by remember(booking.id) { mutableStateOf(false) }
    // Local chat overlay state
    var showFullChat by remember(booking.id) { mutableStateOf(false) }

    // Driver phone resolution
    var driverPhone by remember(booking.assignedDriverId, driver) { mutableStateOf("") }

    // One-time dialog when a driver is found (passenger side only)
    var showDriverFoundDialog by remember(booking.id) { mutableStateOf(false) }
    var driverFoundDialogShown by remember(booking.id) { mutableStateOf(false) }

    // Prefer provided driver object
    LaunchedEffect(driver) {
        if (driver?.phoneNumber?.isNotBlank() == true) {
            driverPhone = driver.phoneNumber
        }
    }

    // Try to fetch from users table via profile Flow
    LaunchedEffect(booking.assignedDriverId) {
        if (booking.assignedDriverId.isNotEmpty()) {
            try {
                bookingViewModel.getUserProfile(booking.assignedDriverId).collect { profile ->
                    val pn = profile?.phoneNumber.orEmpty()
                    if (pn.isNotBlank()) {
                        driverPhone = pn
                    }
                }
            } catch (_: Exception) {
                // ignore, fallback below
            }
        }
    }

    // Fallback to hardware drivers table lookup if still blank
    LaunchedEffect(booking.assignedDriverId, driverPhone) {
        if (driverPhone.isBlank() && booking.assignedDriverId.isNotEmpty()) {
            try {
                val result = bookingViewModel.getDriverById(booking.assignedDriverId)
                result.fold(
                    onSuccess = { data ->
                        val pn = (data["phoneNumber"] as? String)
                            ?: (data["phone"] as? String)
                            ?: (data["contactNumber"] as? String)
                            ?: (data["contact"] as? String)
                            ?: ""
                        if (pn.isNotBlank()) driverPhone = pn
                    },
                    onFailure = { /* no-op */ }
                )
            } catch (_: Exception) {
                // no-op
            }
        }
    }

    LaunchedEffect(key1 = booking.id, key2 = booking.status) {
        if (booking.status == BookingStatus.PENDING) {
            // Wait 2 minutes
            kotlinx.coroutines.delay(120_000L)
            // If still pending after the delay, auto-cancel immediately and show dialog
            if (booking.status == BookingStatus.PENDING && !autoCancelled) {
                autoCancelled = true
                // Stop polling immediately before issuing cancel
                bookingViewModel.stopBookingPolling(booking.id)
                onCancelBooking(booking.id)
                showNoDriversDialog = true
            }
        } else {
            showNoDriversDialog = false
            autoCancelled = false
        }
    }

    // Detect transition to ACCEPTED/assigned and show a one-time dialog for passengers
    LaunchedEffect(booking.status, booking.assignedDriverId) {
        val isAssigned = booking.assignedDriverId.isNotEmpty() || booking.status == BookingStatus.ACCEPTED || booking.status == BookingStatus.IN_PROGRESS
        if (currentUser.userType == UserType.PASSENGER && isAssigned && !driverFoundDialogShown) {
            driverFoundDialogShown = true
            showDriverFoundDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Booking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Passenger: call the driver if phone available
                    if (currentUser.userType == UserType.PASSENGER && driverPhone.isNotBlank()) {
                        IconButton(onClick = {
                            val sanitized = driverPhone.filter { it.isDigit() || it == '+' }
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = "tel:$sanitized".toUri()
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Call Driver"
                            )
                        }
                    }
                    // Driver: call the customer if phone available
                    else if (currentUser.userType == UserType.DRIVER && booking.phoneNumber.isNotBlank()) {
                        IconButton(onClick = {
                            val sanitized = booking.phoneNumber.filter { it.isDigit() || it == '+' }
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = "tel:$sanitized".toUri()
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Call Customer"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ChatFloatingActionButton(
                booking = booking,
                currentUser = currentUser,
                onOpenChat = {
                    // Open local overlay chat to ensure navigation works immediately
                    showFullChat = true
                    // Also call external navigation callback if the host wants to handle it
                    onNavigateToFullChat()
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Map showing current pickup and destination
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Trip Map",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OSMMapView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            pickupLocation = booking.pickupGeoPoint,
                            dropoffLocation = booking.dropoffGeoPoint,
                            onMapClick = {},
                            onPickupLocationDragged = {},
                            onDropoffLocationDragged = {},
                            onInvalidLocationSelected = {},
                            restrictToBarangay177 = true,
                            enableZoom = true,
                            enableDrag = false,
                            validateBarangay177 = false,
                            routingService = null
                        )
                    }
                }
            }

            // Additional booking information
            item {
                BookingDetailsCard(booking = booking)
            }

            // Safety information
            item {
                SafetyCard()
            }

            // Waiting indicator when booking is pending
            if (booking.status == BookingStatus.PENDING) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Waiting for drivers...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Your request has been sent. We'll notify you once a driver accepts.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cancel button
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(onClick = {
                                    // Stop polling immediately on manual cancel
                                    bookingViewModel.stopBookingPolling(booking.id)
                                    onCancelBooking(booking.id)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancel request")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Timeout dialog after auto-cancellation
    if (showNoDriversDialog) {
        AlertDialog(
            onDismissRequest = { showNoDriversDialog = false },
            title = { Text("No drivers available") },
            text = { Text("There are no available drivers at the moment. Your booking request has been cancelled.") },
            confirmButton = {
                TextButton(onClick = { showNoDriversDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Driver found dialog (auto-dismiss)
    if (showDriverFoundDialog) {
        LaunchedEffect(Unit) {
            // Auto-dismiss after a short moment to feel like a transition cue
            kotlinx.coroutines.delay(1_800)
            showDriverFoundDialog = false
        }
        AlertDialog(
            onDismissRequest = { showDriverFoundDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("We found you a driver") },
            text = { Text("Connecting you to your driver…") },
            confirmButton = {
                TextButton(onClick = { showDriverFoundDialog = false }) {
                    Text("Great")
                }
            }
        )
    }

    // Full chat overlay as a full-screen Dialog to capture all touch input
    if (showFullChat) {
        Dialog(
            onDismissRequest = { showFullChat = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Use the same chat implementation as CustomerInterface to load real messages
                SimpleChatScreen(
                    user = currentUser,
                    booking = booking,
                    onBack = { showFullChat = false }
                )
            }
        }
    }
}

@Composable
private fun BookingDetailsCard(
    booking: Booking,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Trip Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(
                icon = Icons.Default.LocationOn,
                label = "Pickup",
                value = booking.pickupLocation
            )

            DetailRow(
                icon = Icons.Default.Flag,
                label = "Destination",
                value = booking.destination
            )

            DetailRow(
                icon = Icons.Default.AttachMoney,
                label = "Estimated Fare",
                value = "₱${String.format(Locale.getDefault(), "%.2f", booking.estimatedFare)}"
            )

            if (booking.verificationCode.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.Security,
                    label = "Verification Code",
                    value = booking.verificationCode
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SafetyCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Safety Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "• Share your trip details with a trusted contact\n" +
                        "• Use the chat to communicate with your driver\n" +
                        "• Report any issues immediately using the emergency button\n" +
                        "• Verify the driver and vehicle details before getting in",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
