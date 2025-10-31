package com.example.toda.ui.operator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.toda.data.*
import com.example.toda.service.RegistrationService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverApplicationManagementScreen(
    registrationService: RegistrationService,
    currentUser: User,
    onBack: () -> Unit
) {
    var currentView by remember { mutableStateOf("pending") } // "pending", "all", "details"
    var selectedApplication by remember { mutableStateOf<Driver?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var pendingApplications by remember { mutableStateOf(registrationService.getPendingDriverApplications()) }
    var allApplications by remember { mutableStateOf(registrationService.getAllDriverApplications()) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Refresh data
    fun refreshData() {
        pendingApplications = registrationService.getPendingDriverApplications()
        allApplications = registrationService.getAllDriverApplications()
    }

    when (currentView) {
        "pending", "all" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Driver Applications",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Success/Error messages
                successMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Tab selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentView == "pending",
                        onClick = { currentView = "pending" },
                        label = { Text("Pending (${pendingApplications.size})") },
                        leadingIcon = if (currentView == "pending") {
                            { Icon(Icons.Default.Pending, contentDescription = null) }
                        } else null
                    )

                    FilterChip(
                        selected = currentView == "all",
                        onClick = { currentView = "all" },
                        label = { Text("All Applications (${allApplications.size})") },
                        leadingIcon = if (currentView == "all") {
                            { Icon(Icons.Default.List, contentDescription = null) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Applications list
                val applicationsToShow = if (currentView == "pending") pendingApplications else allApplications

                if (applicationsToShow.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = "No applications",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (currentView == "pending") "No pending applications" else "No applications yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(applicationsToShow) { application ->
                            DriverApplicationCard(
                                application = application,
                                registrationService = registrationService,
                                onViewDetails = {
                                    selectedApplication = application
                                    currentView = "details"
                                },
                                onApprove = {
                                    coroutineScope.launch {
                                        val success = registrationService.approveDriverRegistration(
                                            application.id,
                                            currentUser.id
                                        )
                                        if (success) {
                                            successMessage = "Application approved successfully!"
                                            errorMessage = null
                                            refreshData()
                                        } else {
                                            errorMessage = "Failed to approve application"
                                            successMessage = null
                                        }
                                        // Clear messages after delay
                                        kotlinx.coroutines.delay(3000)
                                        successMessage = null
                                        errorMessage = null
                                    }
                                },
                                onReject = {
                                    selectedApplication = application
                                    showRejectDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
        "details" -> {
            selectedApplication?.let { application ->
                DriverApplicationDetailsScreen(
                    application = application,
                    registrationService = registrationService,
                    onBack = { currentView = "pending" },
                    onApprove = {
                        coroutineScope.launch {
                            val success = registrationService.approveDriverRegistration(
                                application.id,
                                currentUser.id
                            )
                            if (success) {
                                successMessage = "Application approved successfully!"
                                refreshData()
                                currentView = "pending"
                            } else {
                                errorMessage = "Failed to approve application"
                            }
                        }
                    },
                    onReject = {
                        showRejectDialog = true
                    }
                )
            }
        }
    }

    // Rejection dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Application") },
            text = {
                Column {
                    Text("Please provide a reason for rejecting this application:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedApplication?.let { application ->
                            coroutineScope.launch {
                                val success = registrationService.rejectDriverRegistration(
                                    application.id,
                                    rejectionReason,
                                    currentUser.id
                                )
                                if (success) {
                                    successMessage = "Application rejected"
                                    refreshData()
                                } else {
                                    errorMessage = "Failed to reject application"
                                }
                                showRejectDialog = false
                                rejectionReason = ""
                                currentView = "pending"
                            }
                        }
                    },
                    enabled = rejectionReason.isNotBlank()
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        rejectionReason = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DriverApplicationCard(
    application: Driver,
    registrationService: RegistrationService,
    onViewDetails: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = application.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                StatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            ApplicationDetailRow("Phone:", application.phoneNumber)
            ApplicationDetailRow("License:", application.licenseNumber)
            ApplicationDetailRow("Tricycle Plate:", application.tricyclePlateNumber)
            ApplicationDetailRow("TODA ID:", if (application.todaMembershipId.isNotEmpty()) application.todaMembershipId else "Not assigned")

            ApplicationDetailRow(
                "Applied:",
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(application.registrationDate))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }

                if (application.status == DriverStatus.PENDING_APPROVAL) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }

                    Button(
                        onClick = onApprove
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusBadge(status: DriverStatus) {
    val (backgroundColor, textColor) = when (status) {
        DriverStatus.PENDING_APPROVAL -> Color(0xFFFF9800) to Color.White
        DriverStatus.APPROVED -> Color(0xFF4CAF50) to Color.White
        DriverStatus.REJECTED -> Color(0xFFF44336) to Color.White
        DriverStatus.ACTIVE -> Color(0xFF2196F3) to Color.White
        DriverStatus.SUSPENDED -> Color(0xFF9C27B0) to Color.White
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name.replace("_", " "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
private fun DriverApplicationDetailsScreen(
    application: Driver,
    registrationService: RegistrationService,
    onBack: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Application Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Applicant Information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Applicant Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Name:", application.name)
                DetailRow("Phone Number:", application.phoneNumber)
                DetailRow("Address:", application.address)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // License Information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "License Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("License Number:", application.licenseNumber)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle Information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Vehicle Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Tricycle Plate Number:", application.tricyclePlateNumber)
                DetailRow("TODA Membership ID:", if (application.todaMembershipId.isNotEmpty()) application.todaMembershipId else "Not assigned")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Application Status
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = application.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Applied on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(application.registrationDate))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (application.approvedBy.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Processed by:", application.approvedBy)
                    if (application.approvalDate > 0) {
                        DetailRow(
                            "Processed on:",
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(application.approvalDate))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        if (application.status == DriverStatus.PENDING_APPROVAL) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject Application")
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve Application")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}
