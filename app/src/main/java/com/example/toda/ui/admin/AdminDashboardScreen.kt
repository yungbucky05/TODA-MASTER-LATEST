package com.example.toda.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.viewmodel.AdminDiscountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onDriverManagement: () -> Unit,
    onBookingDetails: () -> Unit,
    onContributionTracking: () -> Unit,
    onQueueManagement: () -> Unit,
    onDiscountApplications: () -> Unit,
    onLogout: () -> Unit, // added
    discountViewModel: AdminDiscountViewModel = hiltViewModel()
) {
    val pendingDiscountCount by discountViewModel.pendingCount.collectAsStateWithLifecycle()

    // Get admin functions inside the composable context
    val adminFunctions = remember {
        listOf(
            AdminFunction(
                id = "driver_management",
                title = "Driver Management",
                description = "Manage and verify TODA drivers",
                icon = Icons.Default.Groups
            ),
            AdminFunction(
                id = "booking_details",
                title = "Booking Details",
                description = "View booking history and logs",
                icon = Icons.AutoMirrored.Filled.Assignment
            ),
            AdminFunction(
                id = "contribution_tracking",
                title = "Contributions",
                description = "Track driver contributions",
                icon = Icons.Default.Payments
            ),
            AdminFunction(
                id = "queue_management",
                title = "Queue",
                description = "View and manage driver queue",
                icon = Icons.Default.List
            ),
            AdminFunction(
                id = "discount_applications",
                title = "Discount Applications",
                description = "Review and approve discount requests",
                icon = Icons.Default.Discount
            )
        )
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
                text = "TODA Admin Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Welcome to TODA Administration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Barangay 177, Camarin, Caloocan City",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Admin Functions Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(adminFunctions) { function ->
                AdminFunctionCard(
                    adminFunction = function,
                    iconColor = when(function.id) {
                        "driver_management" -> MaterialTheme.colorScheme.primary
                        "booking_details" -> MaterialTheme.colorScheme.secondary
                        "contribution_tracking" -> MaterialTheme.colorScheme.tertiary
                        "discount_applications" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    badgeCount = if (function.id == "discount_applications") pendingDiscountCount else null,
                    onClick = {
                        when (function.id) {
                            "driver_management" -> onDriverManagement()
                            "booking_details" -> onBookingDetails()
                            "contribution_tracking" -> onContributionTracking()
                            "queue_management" -> onQueueManagement()
                            "discount_applications" -> onDiscountApplications()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminFunctionCard(
    adminFunction: AdminFunction,
    iconColor: Color,
    onClick: () -> Unit,
    badgeCount: Int? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = adminFunction.icon,
                    contentDescription = adminFunction.title,
                    modifier = Modifier.size(48.dp),
                    tint = iconColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = adminFunction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = adminFunction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge for pending count
            if (badgeCount != null && badgeCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class AdminFunction(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)
