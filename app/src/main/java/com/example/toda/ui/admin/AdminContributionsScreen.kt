package com.example.toda.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.toda.data.FirebaseContribution
import com.example.toda.ui.components.DatePicker
import com.example.toda.viewmodel.ContributionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContributionsScreen(
    onBack: () -> Unit,
    viewModel: ContributionsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val contributions by viewModel.filteredContributions.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val weekTotal by viewModel.weekTotal.collectAsState()
    val monthTotal by viewModel.monthTotal.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDriver by viewModel.selectedDriver.collectAsState()
    val driverStats by viewModel.driverStats.collectAsState()
    val availableDrivers by viewModel.availableDrivers.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showSearchSuggestions by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }

    // Filter drivers based on search query
    val filteredDrivers = remember(availableDrivers, searchQuery) {
        val result = if (searchQuery.isBlank()) {
            // Show all drivers when search field is active but empty
            availableDrivers.take(10) // Limit to first 10 for performance
        } else {
            availableDrivers.filter { driver ->
                driver.driverName.contains(searchQuery, ignoreCase = true) ||
                driver.driverId.contains(searchQuery, ignoreCase = true)
            }
        }

        // Debug logging
        println("AdminContributionsScreen: Search query = '$searchQuery'")
        println("AdminContributionsScreen: Available drivers count = ${availableDrivers.size}")
        println("AdminContributionsScreen: Filtered drivers count = ${result.size}")
        result.forEach { driver ->
            println("  - ${driver.driverName} (${driver.driverId})")
        }

        result
    }

    // Update to show suggestions when focused or when there's text
    LaunchedEffect(isSearchFocused, searchQuery) {
        showSearchSuggestions = isSearchFocused || searchQuery.isNotBlank()
    }

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
                text = "Contributions History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Cards - Show either overall or driver-specific stats
        if (selectedDriver == null) {
            // Overall Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Today",
                    amount = viewModel.getFormattedAmount(todayTotal),
                    icon = Icons.Default.Today,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "This Week",
                    amount = viewModel.getFormattedAmount(weekTotal),
                    icon = Icons.Default.DateRange,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "This Month",
                    amount = viewModel.getFormattedAmount(monthTotal),
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Driver-Specific Stats
            driverStats?.let { stats ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stats.driverName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "RFID: ${stats.driverId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${stats.totalContributions} contributions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.downloadDriverReport(context, stats.driverName)
                                }
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Download Report",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DriverStatCard(
                                title = "Today",
                                amount = viewModel.getFormattedAmount(stats.todayTotal),
                                modifier = Modifier.weight(1f)
                            )
                            DriverStatCard(
                                title = "This Week",
                                amount = viewModel.getFormattedAmount(stats.weekTotal),
                                modifier = Modifier.weight(1f)
                            )
                            DriverStatCard(
                                title = "This Month",
                                amount = viewModel.getFormattedAmount(stats.monthTotal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search and Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.updateSearchQuery(it)
                    },
                    placeholder = { Text("Search driver name or RFID...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isSearchFocused = focusState.isFocused
                        },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.clearFilters()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                )

                // Search suggestions dropdown
                if (showSearchSuggestions && filteredDrivers.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(filteredDrivers) { driver ->
                                TextButton(
                                    onClick = {
                                        viewModel.updateDriverFilter(driver.driverName) // Pass driver NAME instead of RFID
                                        viewModel.updateSearchQuery(driver.driverName)
                                        showSearchSuggestions = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = driver.driverName,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "RFID: ${driver.driverId}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                if (driver != filteredDrivers.last()) {
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }

            FilledTonalButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Filter Date")
            }
        }

        // Active Filters
        if (selectedDate != null || selectedDriver != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedDate != null) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.updateDateFilter(null) },
                        label = { Text(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(selectedDate!!)) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Clear date", modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (selectedDriver != null) {
                    FilterChip(
                        selected = true,
                        onClick = {
                            viewModel.updateDriverFilter(null)
                            viewModel.updateSearchQuery("")
                        },
                        label = { Text(driverStats?.driverName ?: "Driver") },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Clear driver", modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (selectedDate != null || selectedDriver != null) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("Clear All")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contributions List
        if (contributions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No contributions found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = contributions,
                    key = { "${it.driverId}_${it.timestamp}" }
                ) { contribution ->
                    ContributionCard(contribution = contribution)
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePicker(
            onDateSelected = {
                viewModel.updateDateFilter(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DriverStatCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ContributionCard(contribution: FirebaseContribution) {
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
                Column {
                    Text(
                        text = contribution.driverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RFID: ${contribution.driverId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = "₱${String.format(Locale.US, "%.2f", contribution.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(contribution.timestamp * 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Source: ${contribution.source.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
