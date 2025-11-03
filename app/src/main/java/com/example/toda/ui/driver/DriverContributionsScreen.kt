package com.example.toda.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.toda.data.*
import com.example.toda.viewmodel.EnhancedBookingViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverContributionsScreen(
    driverId: String,
    viewModel: EnhancedBookingViewModel = hiltViewModel()
) {
    var contributions by remember { mutableStateOf<List<FirebaseContribution>>(emptyList()) }
    var contributionSummary by remember { mutableStateOf<ContributionSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Today", "This Week", "This Month"

    // Payment mode and balance state - KEEP FOR DISPLAY
    var paymentMode by remember { mutableStateOf<String?>(null) }
    var driverBalance by remember { mutableStateOf(0.0) }
    var showPaymentModeDialog by remember { mutableStateOf(false) }
    var isRestoringPaymentMode by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Load contributions and payment data on first load
    LaunchedEffect(driverId) {
        isLoading = true
        try {
            // Get all contributions
            val contributionsResult = viewModel.getDriverContributions(driverId)
            contributionsResult.fold(
                onSuccess = { allContributions ->
                    contributions = allContributions
                },
                onFailure = { error ->
                    errorMessage = "Failed to load contributions: ${error.message}"
                }
            )

            // Get contribution summary
            val summaryResult = viewModel.getDriverContributionSummary(driverId)
            summaryResult.fold(
                onSuccess = { summary ->
                    contributionSummary = summary
                },
                onFailure = { error ->
                    println("Failed to load contribution summary: ${error.message}")
                }
            )
        } catch (e: Exception) {
                println("Failed to load driver contributions: ${e.message}")
                errorMessage = "Unexpected error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Real-time observer for payment mode - sync with dashboard changes
    LaunchedEffect(driverId) {
        if (driverId.isNotEmpty()) {
            viewModel.observeDriverPaymentMode(driverId).collect { mode ->
                println("=== CONTRIBUTIONS: PAYMENT MODE UPDATE ===")
                println("Driver $driverId payment mode changed to: $mode")
                paymentMode = mode
            }
        }
    }

    // Real-time observer for driver balance - auto-update when balance changes
    LaunchedEffect(driverId) {
        if (driverId.isNotEmpty()) {
            viewModel.observeDriverBalance(driverId).collect { balance ->
                println("=== CONTRIBUTIONS: BALANCE UPDATE ===")
                println("Driver $driverId balance changed to: ₱$balance")
                driverBalance = balance
            }
        }
    }

    // Auto-restore payment mode when balance clears in pay_balance mode
    LaunchedEffect(driverBalance, paymentMode) {
        if (!isRestoringPaymentMode && paymentMode == "pay_balance" && driverBalance <= 0.0) {
            isRestoringPaymentMode = true
            println("=== AUTO RESTORE PAYMENT MODE TRIGGERED ===")
            println("Balance cleared; restoring preferred payment mode...")
            viewModel.restorePreferredPaymentMode(driverId).fold(
                onSuccess = { restoredMode ->
                    println("✓ Payment mode restored to $restoredMode after balance cleared")
                    paymentMode = restoredMode
                },
                onFailure = { error ->
                    println("✗ Failed to restore payment mode: ${error.message}")
                }
            )
            isRestoringPaymentMode = false
        }
    }

    val filteredContributions = remember(contributions, selectedFilter) {
        when (selectedFilter) {
            "Today" -> {
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                contributions.filter { it.timestamp >= todayStart }
            }
            "This Week" -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                contributions.filter { it.timestamp >= weekStart }
            }
            "This Month" -> {
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                contributions.filter { it.timestamp >= monthStart }
            }
            else -> contributions
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1976D2))
            }
        } else if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            // Payment Mode and Balance Card
            PaymentModeAndBalanceCard(
                paymentMode = paymentMode ?: "pay_every_trip",
                balance = driverBalance,
                onChangePaymentMode = { showPaymentModeDialog = true },
                onMarkPayBalance = {
                    coroutineScope.launch {
                        if (driverBalance <= 0) {
                            println("⚠ No outstanding balance to pay")
                            return@launch
                        }

                        viewModel.updatePaymentMode(driverId, "pay_balance").fold(
                            onSuccess = {
                                paymentMode = "pay_balance"
                                println("✓ Payment mode set to pay_balance; please settle at the terminal")
                            },
                            onFailure = { error ->
                                println("✗ Error enabling pay_balance: ${error.message}")
                                android.widget.Toast.makeText(
                                    context,
                                    "Error switching to pay balance: ${error.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                },
                payBalanceMarked = (paymentMode == "pay_balance")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contributions Summary Section
            contributionSummary?.let { summary ->
                ContributionSummaryCard(summary = summary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Filter Tabs
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contributions List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contributions History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "${filteredContributions.size} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contributions List
            if (filteredContributions.isEmpty()) {
                EmptyContributionsCard(filter = selectedFilter)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredContributions) { contribution ->
                        ContributionItem(contribution = contribution)
                    }
                }
            }
        }
    }

    // Payment Mode Selection Dialog
    if (showPaymentModeDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentModeDialog = false },
            title = { Text("Select Payment Mode") },
            text = {
                Column {
                    PaymentModeOption(
                        title = "Pay Every Trip",
                        description = "Pay ₱5.00 before each trip starts",
                        isSelected = paymentMode == "pay_every_trip",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.updatePaymentMode(driverId, "pay_every_trip").fold(
                                    onSuccess = {
                                        paymentMode = "pay_every_trip"
                                        showPaymentModeDialog = false
                                    },
                                    onFailure = { error ->
                                        println("Error updating payment mode: ${error.message}")
                                    }
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PaymentModeOption(
                        title = "Pay Later",
                        description = "Accumulate ₱5.00 per trip, pay at end of day",
                        isSelected = paymentMode == "pay_later",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.updatePaymentMode(driverId, "pay_later").fold(
                                    onSuccess = {
                                        paymentMode = "pay_later"
                                        showPaymentModeDialog = false
                                    },
                                    onFailure = { error ->
                                        println("Error updating payment mode: ${error.message}")
                                    }
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PaymentModeOption(
                        title = "Pay Balance Now",
                        description = "Switch kiosk to balance payoff mode",
                        isSelected = paymentMode == "pay_balance",
                        enabled = driverBalance > 0,
                        onClick = {
                            coroutineScope.launch {
                                if (driverBalance <= 0) {
                                    println("⚠ No outstanding balance to settle")
                                    return@launch
                                }

                                viewModel.updatePaymentMode(driverId, "pay_balance").fold(
                                    onSuccess = {
                                        paymentMode = "pay_balance"
                                        showPaymentModeDialog = false
                                    },
                                    onFailure = { error ->
                                        println("Error updating payment mode: ${error.message}")
                                    }
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaymentModeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ContributionSummaryCard(summary: ContributionSummary) {

    var showContributionsInfo by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Contribution Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                IconButton(onClick = { showContributionsInfo = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ContributionSummaryItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    value = "₱${summary.totalContributions.toInt()}",
                    label = "Total",
                    iconColor = Color(0xFF4CAF50)
                )
                ContributionSummaryItem(
                    icon = Icons.Default.Today,
                    value = "₱${summary.todayContributions.toInt()}",
                    label = "Today",
                    iconColor = Color(0xFF2196F3)
                )
                ContributionSummaryItem(
                    icon = Icons.Default.CalendarMonth,
                    value = "₱${summary.thisMonthContributions.toInt()}",
                    label = "This Month",
                    iconColor = Color(0xFF9C27B0)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Records",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${summary.contributionCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
                Column {
                    Text(
                        text = "Streak Days",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${summary.streak}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
                Column {
                    Text(
                        text = "Daily Average",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "₱${summary.averageDailyContribution.toInt()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }
        }
    }

    // Info Dialog
    if (showContributionsInfo) {
        AlertDialog(
            onDismissRequest = { showContributionsInfo = false },
            confirmButton = {
                TextButton(onClick = { showContributionsInfo = false }) {
                    Text("Got it")
                }
            },
            title = { Text("Driver Contributions") },
            text = {
                Text(
                    "This section shows your contribution records for the day, including total butaw payments, payment dates, and confirmation status. " +
                            "You can monitor your active and completed contributions here to stay updated on your TODA balance."
                )
            }
        )
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Today", "This Week", "This Month")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                selected = selectedFilter == filter,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ContributionItem(contribution: FirebaseContribution) {
    val isPayLaterContribution = contribution.paid == false
    val isAdminAdjustment = contribution.contributionType == "ADMIN_ADJUSTMENT"

    val contributionTitle = when {
        isAdminAdjustment -> "Admin Adjustment"
        isPayLaterContribution -> "Pay Later Contribution"
        else -> "Pay Every Trip Contribution"
    }

    val contributionIcon = when {
        isAdminAdjustment -> Icons.Default.AdminPanelSettings
        isPayLaterContribution -> Icons.Default.Schedule
        else -> Icons.Default.MonetizationOn
    }

    val iconTint = when {
        isAdminAdjustment -> Color(0xFF9C27B0)
        isPayLaterContribution -> Color(0xFFFFA000)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contribution Type Icon
            Icon(
                imageVector = contributionIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Contribution Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contributionTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Text(
                    text = formatTimestamp(contribution.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (isPayLaterContribution) {
                    Text(
                        text = "Marked as pay later (unpaid)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFA000),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (contribution.notes.isNotEmpty()) {
                    Text(
                        text = contribution.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₱${contribution.amount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                if (contribution.deviceId.isNotEmpty() && contribution.deviceId != "default") {
                    Text(
                        text = "Device: ${contribution.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContributionsCard(filter: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Contributions Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = when (filter) {
                    "Today" -> "You haven't made any contributions today"
                    "This Week" -> "No contributions this week"
                    "This Month" -> "No contributions this month"
                    else -> "Start contributing to track your payments here"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ContributionSummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
        diff < 86400_000 -> "${diff / 3600_000}h ago" // Less than 1 day
        diff < 604800_000 -> "${diff / 86400_000}d ago" // Less than 1 week
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}

@Composable
private fun PaymentModeAndBalanceCard(
    paymentMode: String,
    balance: Double,
    onChangePaymentMode: () -> Unit,
    onMarkPayBalance: () -> Unit = {},
    payBalanceMarked: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (balance > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = when (paymentMode) {
                            "pay_later" -> "Pay Later"
                            "pay_balance" -> "Pay Balance Now"
                            "pay_every_trip" -> "Pay Every Trip"
                            else -> paymentMode.replaceFirstChar { it.uppercase() }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                TextButton(onClick = onChangePaymentMode) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "₱%.2f".format(balance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (balance > 0) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                    )
                }
            }

            // Pay Balance Button - only show if balance > 0
            if (balance > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                if (payBalanceMarked) {
                    // Show "Waiting for Payment" status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Waiting for payment at terminal...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Show "I Want to Pay" button
                    Button(
                        onClick = onMarkPayBalance,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I Want to Pay Balance")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (payBalanceMarked)
                                "Go to the terminal to settle your balance"
                            else
                                "Outstanding balance from completed trips. Click button above when ready to pay at terminal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentModeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !enabled -> Color(0xFFF5F5F5)
                isSelected -> Color(0xFF1976D2)
                else -> Color.White
            }
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = if (enabled) 1f else 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (isSelected) Color.White else Color(0xFF1976D2),
                    disabledSelectedColor = Color.White.copy(alpha = 0.3f),
                    disabledUnselectedColor = Color.Gray.copy(alpha = 0.4f)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        !enabled -> Color.Gray
                        isSelected -> Color.White
                        else -> Color.Black
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        !enabled -> Color.Gray.copy(alpha = 0.6f)
                        isSelected -> Color.White.copy(alpha = 0.9f)
                        else -> Color.Gray
                    }
                )
            }
        }
    }
}
