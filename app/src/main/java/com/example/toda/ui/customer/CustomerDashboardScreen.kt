package com.example.toda.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.data.UserProfile
import com.example.toda.viewmodel.CustomerDashboardViewModel
import com.example.toda.viewmodel.RideSummaryCounts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    userId: String,
    onBookRide: () -> Unit,
    onViewProfile: () -> Unit,
    onLogout: () -> Unit,
    onApplyDiscount: (String) -> Unit, // Navigate to discount form with category
    viewModel: CustomerDashboardViewModel = hiltViewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val rideSummary by viewModel.rideSummary.collectAsStateWithLifecycle()

    var showDiscountCategoryDialog by remember { mutableStateOf(false) }
    var showPendingApplicationDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadUserData(userId)
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Help, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("How to use TODA")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "- Tap Book a Ride to request a driver.\n" +
                            "- Review Passenger Details to confirm your contact info.\n" +
                            "- Apply for Discount if you qualify for special fares.\n" +
                            "- Check Ride Summary for your recent trips.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // Pending Application Dialog
    if (showPendingApplicationDialog) {
        val profile = userProfile
        if (profile?.discountType != null) {
            AlertDialog(
                onDismissRequest = { showPendingApplicationDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Pending Application") },
                text = {
                    Column {
                        Text(
                            text = "You have a pending application for:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = profile.discountType.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please wait for admin verification. You will be notified once your application is reviewed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showPendingApplicationDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    // Discount Category Selection Dialog
    if (showDiscountCategoryDialog) {
        DiscountCategoryDialog(
            onDismiss = { showDiscountCategoryDialog = false },
            onSelectCategory = { category ->
                showDiscountCategoryDialog = false
                onApplyDiscount(category)
            }
        )
    }

    val showDiscountAction = userProfile?.discountVerified != true
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.surface
    )
    val handleDiscountClick: () -> Unit = {
        val profile = userProfile
        if (profile?.discountType != null && !profile.discountVerified) {
            showPendingApplicationDialog = true
        } else {
            showDiscountCategoryDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            dashboardState.error?.let { error ->
                item {
                    DashboardMessageCard(
                        text = error,
                        icon = Icons.Default.Error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            dashboardState.message?.let { message ->
                item {
                    DashboardMessageCard(
                        text = message,
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            item {
                DashboardHeader(
                    profile = userProfile,
                    onBookRide = onBookRide,
                    onShowHelp = { showHelpDialog = true }
                )
            }

            if (showDiscountAction) {
                item {
                    OutlinedButton(
                        onClick = handleDiscountClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Discount, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply for Discount")
                    }
                }
            }

            item {
                StatsSection(rideSummary = rideSummary)
            }

            item {
                CustomerInformationCard(userProfile = userProfile)
            }

            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun DashboardMessageCard(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    profile: UserProfile?,
    onBookRide: () -> Unit,
    onShowHelp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0E8E90), // TealBlue80 - Main teal
                        Color(0xFF0B7476)  // TealBlue60 - Slightly darker teal
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = profile?.name?.ifBlank { "Passenger" } ?: "Passenger",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderInfoChip(
                    icon = Icons.Default.Phone,
                    label = profile?.phoneNumber?.takeIf { it.isNotBlank() } ?: "No phone"
                )
            }
            Button(
                onClick = onBookRide,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0E8E90)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color(0xFF0E8E90)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Book a Ride",
                    color = Color(0xFF0E8E90),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(
            onClick = onShowHelp,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Help,
                contentDescription = "How to use the app",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun HeaderInfoChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatsSection(rideSummary: RideSummaryCounts) {
    DashboardSectionCard(
        title = "Ride Summary",
        subtitle = "Your recent booking stats"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Completed",
                value = rideSummary.completed.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Ongoing",
                value = rideSummary.ongoing.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Cancelled",
                value = rideSummary.cancelled.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CustomerInformationCard(userProfile: UserProfile?) {
    val phone = userProfile?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not provided"
    val address = userProfile?.address?.takeIf { it.isNotBlank() }

    DashboardSectionCard(
        title = "Passenger Details",
        subtitle = address
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoItem(label = "Mobile Number", value = phone)
            HorizontalDivider()
            DiscountStatusRow(userProfile = userProfile)
            AccountStatusBanner(userProfile = userProfile)
        }
    }
}

@Composable
private fun DiscountStatusRow(userProfile: UserProfile?) {
    val discountType = userProfile?.discountType
    val verified = userProfile?.discountVerified == true

    val (containerColor, contentColor, message) = when {
        discountType != null && verified -> Triple(
            Color(0xFFE0F7F7), // Light teal
            Color(0xFF0E8E90), // Main teal
            "${discountType.displayName} discount is active"
        )
        discountType != null -> Triple(
            Color(0xFFF5F5F5), // Light gray
            Color(0xFF757575), // Medium gray
            "${discountType.displayName} pending verification"
        )
        else -> Triple(
            Color(0xFFF5F5F5), // Light gray
            Color(0xFF757575), // Medium gray
            "No discount applied"
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (verified) Icons.Default.Verified else Icons.Default.Discount,
                contentDescription = null,
                tint = contentColor
            )
            Column {
                Text(
                    text = "Discount",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                if (discountType != null && userProfile?.discountIdNumber?.isNotBlank() == true) {
                    Text(
                        text = "ID: ${userProfile.discountIdNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountStatusBanner(userProfile: UserProfile?) {
    val isBlocked = userProfile?.isBlocked == true
    val (containerColor, contentColor, message) = if (isBlocked) {
        Triple(
            Color(0xFFFFEBEE), // Light red
            Color(0xFFD32F2F), // Red
            "Contact support to restore booking access."
        )
    } else {
        Triple(
            Color(0xFFE0F7F7), // Light teal
            Color(0xFF0E8E90), // Main teal
            "You're all set to request rides."
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.Verified,
                contentDescription = null,
                tint = contentColor
            )
            Column {
                Text(
                    text = if (isBlocked) "Account Status: Blocked" else "Account Status: Active",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun DashboardSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                actions?.let { actionContent ->
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actionContent
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = Color(0xFFE0F7F7) // Very light teal
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0E8E90) // Main teal
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF0B7476) // Darker teal
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DiscountCategoryDialog(
    onDismiss: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Discount, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Discount Category")
            }
        },
        text = {
            Column {
                Text(
                    text = "Choose the discount category that applies to you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PWD Discount Card
                DiscountCategoryCard(
                    title = "Person with Disability (PWD)",
                    discount = "20% discount",
                    icon = Icons.Default.Accessible,
                    onClick = { onSelectCategory("PWD") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Senior Citizen Card
                DiscountCategoryCard(
                    title = "Senior Citizen",
                    discount = "20% discount",
                    icon = Icons.Default.Elderly,
                    onClick = { onSelectCategory("SENIOR_CITIZEN") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Student Card
                DiscountCategoryCard(
                    title = "Student",
                    discount = "10% discount",
                    icon = Icons.Default.School,
                    onClick = { onSelectCategory("STUDENT") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DiscountCategoryCard(
    title: String,
    discount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = discount,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
