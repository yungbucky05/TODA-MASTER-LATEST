package com.example.toda.ui.customer

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.example.toda.data.DriverFlag
import com.example.toda.data.FlagStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Customer Flag Status Banner - Shows at top of customer interface
 * User-friendly design with clear messaging and helpful icons
 */
@Composable
fun CustomerFlagStatusBanner(
    flagStatus: FlagStatus,
    flagScore: Int,
    activeFlags: List<DriverFlag>,
    modifier: Modifier = Modifier
) {
    if (flagStatus == FlagStatus.GOOD) return

    val bannerConfig = when (flagStatus) {
        FlagStatus.MONITORED -> BannerConfig(
            backgroundColor = Color(0xFFFFF3CD),
            borderColor = Color(0xFFFFC107),
            icon = "👀",
            title = "Account Under Monitoring",
            message = "We've noticed some issues with your recent bookings. Please improve your booking behavior to maintain full access.",
            iconColor = Color(0xFFFFA000)
        )
        FlagStatus.RESTRICTED -> BannerConfig(
            backgroundColor = Color(0xFFFFF3E0),
            borderColor = Color(0xFFFF9800),
            icon = "⚠️",
            title = "Limited Account Access",
            message = "Your account has restrictions due to multiple issues. You have limited bookings per day and may need to prepay.",
            iconColor = Color(0xFFFF6F00)
        )
        FlagStatus.SUSPENDED -> BannerConfig(
            backgroundColor = Color(0xFFFFEBEE),
            borderColor = Color(0xFFD32F2F),
            icon = "🚫",
            title = "Account Suspended",
            message = "Your account is temporarily suspended. You cannot create bookings at this time. Please contact support to resolve this.",
            iconColor = Color(0xFFC62828)
        )
        else -> return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = bannerConfig.backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = bannerConfig.icon,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .background(
                            color = bannerConfig.iconColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bannerConfig.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerConfig.iconColor
                    )
                    Text(
                        text = "$flagScore points • ${activeFlags.size} active issue${if (activeFlags.size != 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Message
            Text(
                text = bannerConfig.message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Divider
            HorizontalDivider(color = bannerConfig.borderColor.copy(alpha = 0.3f))

            // What you can do section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "What you can do:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                
                when (flagStatus) {
                    FlagStatus.MONITORED -> {
                        BulletPoint("Show up for your scheduled bookings")
                        BulletPoint("Ensure payment is ready at dropoff")
                        BulletPoint("Double-check pickup locations before booking")
                    }
                    FlagStatus.RESTRICTED -> {
                        BulletPoint("Complete upcoming bookings successfully")
                        BulletPoint("Pay for rides on time")
                        BulletPoint("Prepayment may be required for new bookings")
                    }
                    FlagStatus.SUSPENDED -> {
                        BulletPoint("Contact support to discuss your account")
                        BulletPoint("Resolve any outstanding issues or payments")
                        BulletPoint("Wait for admin review and approval")
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
    }
}

private data class BannerConfig(
    val backgroundColor: Color,
    val borderColor: Color,
    val icon: String,
    val title: String,
    val message: String,
    val iconColor: Color
)

/**
 * Customer Flag Status Badge - Compact status indicator
 */
@Composable
fun CustomerFlagStatusBadge(
    flagStatus: FlagStatus,
    flagScore: Int,
    modifier: Modifier = Modifier
) {
    val (icon, label, backgroundColor, textColor) = when (flagStatus) {
        FlagStatus.GOOD -> FlagBadgeConfig("✅", "Good Standing", Color(0xFF4CAF50), Color.White)
        FlagStatus.MONITORED -> FlagBadgeConfig("👀", "Monitored", Color(0xFFFFA000), Color.White)
        FlagStatus.RESTRICTED -> FlagBadgeConfig("⚠️", "Restricted", Color(0xFFFF6F00), Color.White)
        FlagStatus.SUSPENDED -> FlagBadgeConfig("🚫", "Suspended", Color(0xFFD32F2F), Color.White)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = "$label • ${flagScore}pts",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private data class FlagBadgeConfig(
    val icon: String,
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
)

/**
 * Customer Active Flags List - Shows all active issues
 */
@Composable
fun CustomerActiveFlagsList(
    flags: List<DriverFlag>,
    modifier: Modifier = Modifier
) {
    if (flags.isEmpty()) {
        // No flags - show success message
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎉",
                    fontSize = 40.sp,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                )
                Column {
                    Text(
                        text = "No Active Issues!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "You're a great passenger! Keep it up! 🌟",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(flags) { flag ->
            CustomerFlagItem(flag = flag)
        }
    }
}

/**
 * Customer Flag Item - Individual flag card with friendly messaging
 */
@Composable
fun CustomerFlagItem(
    flag: DriverFlag,
    modifier: Modifier = Modifier
) {
    val flagTypeInfo = getCustomerFlagTypeInfo(flag.type)
    val severityColor = when (flag.severity.lowercase()) {
        "low" -> Color(0xFF4CAF50)
        "medium" -> Color(0xFFFFA000)
        "high" -> Color(0xFFFF6F00)
        "critical" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = flagTypeInfo.icon,
                        fontSize = 28.sp,
                        modifier = Modifier
                            .background(
                                color = severityColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    )
                    
                    Column {
                        Text(
                            text = flagTypeInfo.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = flag.severity.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = severityColor
                        )
                    }
                }
                
                // Points badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = severityColor
                ) {
                    Text(
                        text = "+${flag.points}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Friendly explanation
            Text(
                text = flagTypeInfo.explanation,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // What to do section
            if (flagTypeInfo.actionItems.isNotEmpty()) {
                HorizontalDivider(color = severityColor.copy(alpha = 0.2f))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "How to improve:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                    
                    flagTypeInfo.actionItems.forEach { action ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✓",
                                fontSize = 13.sp,
                                color = severityColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = action,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Timestamp
            Text(
                text = "Flagged: ${formatTimestamp(flag.timestamp)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Blocked Access Screen for Suspended Customers
 */
@Composable
fun CustomerBlockedAccessScreen(
    userName: String,
    flagScore: Int,
    activeFlags: List<DriverFlag>,
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large warning icon
        Text(
            text = "🚫",
            fontSize = 80.sp,
            modifier = Modifier
                .background(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Account Suspended",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Hi $userName, your account is temporarily suspended due to multiple issues.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Issue summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Account Status",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Points:")
                    Text(
                        text = "$flagScore pts",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active Issues:")
                    Text(
                        text = "${activeFlags.size}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // What this means
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "What this means:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BulletPoint("🚫 You cannot create new bookings")
                    BulletPoint("📞 Support will review your account")
                    BulletPoint("✅ Issues must be resolved to restore access")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Contact support button
        Button(
            onClick = onContactSupport,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Contact Support",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Our support team is here to help you resolve these issues and restore your account.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// Helper functions

private data class CustomerFlagTypeInfo(
    val icon: String,
    val title: String,
    val explanation: String,
    val actionItems: List<String>
)

private fun getCustomerFlagTypeInfo(type: String): CustomerFlagTypeInfo {
    return when (type) {
        "NO_SHOW" -> CustomerFlagTypeInfo(
            icon = "👻",
            title = "No-Show Incidents",
            explanation = "You didn't show up for scheduled bookings. This affects drivers who wait for you and can't accept other rides.",
            actionItems = listOf(
                "Always show up for bookings you make",
                "Cancel early if you can't make it",
                "Set reminders for your scheduled rides"
            )
        )
        "NON_PAYMENT" -> CustomerFlagTypeInfo(
            icon = "💸",
            title = "Payment Issues",
            explanation = "There are outstanding payments for previous rides. All drivers deserve to be paid for their service.",
            actionItems = listOf(
                "Pay for completed rides promptly",
                "Have payment ready before booking",
                "Contact support if there's a payment dispute"
            )
        )
        "WRONG_PIN" -> CustomerFlagTypeInfo(
            icon = "📍",
            title = "Incorrect Pickup Locations",
            explanation = "You've frequently provided wrong pickup locations, making it hard for drivers to find you.",
            actionItems = listOf(
                "Double-check your pickup location before booking",
                "Use familiar landmarks",
                "Enable location services for accurate positioning"
            )
        )
        "ABUSIVE_BEHAVIOR" -> CustomerFlagTypeInfo(
            icon = "😠",
            title = "Behavior Warning",
            explanation = "Drivers reported unprofessional or disrespectful behavior. Everyone deserves to be treated with respect.",
            actionItems = listOf(
                "Treat drivers with courtesy and respect",
                "Be patient and understanding",
                "Communicate politely if there are issues"
            )
        )
        "EXCESSIVE_CANCELLATIONS" -> CustomerFlagTypeInfo(
            icon = "🚫",
            title = "Too Many Cancellations",
            explanation = "You've cancelled too many bookings after they were accepted. This wastes drivers' time and fuel.",
            actionItems = listOf(
                "Only book when you're sure you need the ride",
                "Cancel early if plans change",
                "Consider if you really need the ride before booking"
            )
        )
        else -> CustomerFlagTypeInfo(
            icon = "⚠️",
            title = "Account Issue",
            explanation = "There's an issue with your account that needs attention.",
            actionItems = listOf(
                "Review your recent bookings",
                "Contact support for clarification"
            )
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
