package com.example.toda.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.toda.data.FirebaseBooking
import com.example.toda.ui.components.DatePicker
import com.example.toda.viewmodel.BookingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBookingDetailsScreen(
    onBack: () -> Unit,
    viewModel: BookingsViewModel = viewModel()
) {
    val bookings by viewModel.filteredBookings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Booking History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search and Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search bookings...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            )

            // Status Filter Button
            Box {
                FilledTonalButton(onClick = { showStatusMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter Status")
                }
                DropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { showStatusMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            viewModel.updateStatusFilter(null)
                            showStatusMenu = false
                        }
                    )
                    BookingsViewModel.BOOKING_STATUSES.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status) },
                            onClick = {
                                viewModel.updateStatusFilter(status)
                                showStatusMenu = false
                            }
                        )
                    }
                }
            }

            // Date Filter Button
            FilledTonalButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Filter Date")
            }
        }

        // Active Filters
        if (selectedStatus != null || selectedDate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedStatus?.let {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.updateStatusFilter(null) },
                        label = { Text(it) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Clear status")
                        }
                    )
                }
                selectedDate?.let {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.updateDateFilter(null) },
                        label = { Text(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(it)) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Clear date")
                        }
                    )
                }
                if (selectedStatus != null || selectedDate != null) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("Clear All")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bookings List - Using Column with Scroll instead of LazyColumn
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bookings.forEach { booking ->
                BookingCard(booking = booking)
            }

            // Add some padding at the bottom
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePicker(
            onDateSelected = { viewModel.updateDateFilter(it) },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun BookingCard(booking: FirebaseBooking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Booking #${booking.id.takeLast(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₱${String.format(Locale.US, "%.2f", booking.actualFare)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Customer: ${booking.customerName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "From: ${booking.pickupLocation}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "To: ${booking.destination}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Driver: ${booking.driverName.ifEmpty { "Unassigned" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Status: ${booking.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = when(booking.status) {
                    "COMPLETED" -> MaterialTheme.colorScheme.primary
                    "CANCELLED" -> MaterialTheme.colorScheme.error
                    "IN_PROGRESS" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }
            )
            Text(
                text = "Date: ${SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(booking.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
