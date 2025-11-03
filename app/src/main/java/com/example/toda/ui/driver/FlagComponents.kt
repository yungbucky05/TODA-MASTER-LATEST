package com.example.toda.ui.driver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.toda.data.FlagConstants
import com.example.toda.data.FlagStatusConfig
import java.text.SimpleDateFormat
import java.util.*

/**
 * Flag Status Banner - Shows at top of driver interface
 */
@Composable
fun FlagStatusBanner(
    flagStatus: String,
    flagScore: Int,
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = getFlagStatusConfig(flagStatus, flagScore)
    
    // Only show banner if not in good standing
    if (!config.showBanner) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(config.color).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 2.dp,
                    color = Color(config.color),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on status
            Icon(
                imageVector = when (config.bannerType) {
                    "warning" -> Icons.Default.Warning
                    "error" -> Icons.Default.Error
                    "critical" -> Icons.Default.Block
                    else -> Icons.Default.Info
                },
                contentDescription = config.title,
                tint = Color(config.color),
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${config.icon} ${config.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(config.color)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = config.message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Flag Score: $flagScore points",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(config.color)
                )
            }
        }
        
        // Contact Support Button
        if (config.requiresAction) {
            Button(
                onClick = onContactSupport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(config.color)
                )
            ) {
                Text("Contact Support")
            }
        }
    }
}

/**
 * Flag Status Badge - Shows in profile or status area
 */
@Composable
fun FlagStatusBadge(
    flagStatus: String,
    flagScore: Int,
    modifier: Modifier = Modifier
) {
    val config = getFlagStatusConfig(flagStatus, flagScore)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(config.color).copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = config.icon,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = config.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(config.color)
            )
        }
    }
}

/**
 * Active Flags List - Shows all active flags
 */
@Composable
fun ActiveFlagsList(
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
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "No Flags",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "✅ No Active Flags",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Keep up the great work!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            FlagItem(
                flag = flag
            )
        }
    }
}

/**
 * Individual Flag Item
 */
@Composable
fun FlagItem(
    flag: DriverFlag
) {
    val flagInfo = FlagConstants.getFlagTypeInfo(flag.type)
    val severityColor = getSeverityColor(flag.severity)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = flagInfo.icon,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = flagInfo.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = severityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "+${flag.points} pts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message
            Text(
                text = flagInfo.actionableMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            // Details if available
            if (flag.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        flag.details.forEach { (key, value) ->
                            Text(
                                text = "${key.replace("_", " ").capitalize()}: $value",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Timestamp and severity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(flag.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Text(
                    text = flag.severity.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
            }
        }
    }
}

/**
 * Blocked Access Screen - Shown when driver is suspended
 */
@Composable
fun BlockedAccessScreen(
    flagScore: Int,
    onContactSupport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEBEE)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Account Suspended",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "🚫 Account Suspended",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your account has been suspended due to multiple violations.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Flag Score: $flagScore points",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "You cannot accept bookings until this issue is resolved.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onContactSupport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text(
                        text = "Contact Support Immediately",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Helper function to get flag status configuration
 */
private fun getFlagStatusConfig(status: String, score: Int): FlagStatusConfig {
    return when (status) {
        FlagConstants.STATUS_GOOD -> FlagStatusConfig(
            status = status,
            color = 0xFF4CAF50,
            icon = "✅",
            title = "Good Standing",
            message = "Keep up the great work!",
            showBanner = false
        )
        FlagConstants.STATUS_MONITORED -> FlagStatusConfig(
            status = status,
            color = 0xFFFF9800,
            icon = "👀",
            title = "Account Monitored",
            message = "Please improve your performance to avoid restrictions.",
            showBanner = true,
            bannerType = "warning"
        )
        FlagConstants.STATUS_RESTRICTED -> FlagStatusConfig(
            status = status,
            color = 0xFFF44336,
            icon = "⚠️",
            title = "Account Restricted",
            message = "Your account has limited access. Contact support or resolve active flags.",
            showBanner = true,
            bannerType = "error",
            requiresAction = true
        )
        FlagConstants.STATUS_SUSPENDED -> FlagStatusConfig(
            status = status,
            color = 0xFFD32F2F,
            icon = "🚫",
            title = "Account Suspended",
            message = "Your account is suspended. You cannot accept bookings. Contact admin immediately.",
            showBanner = true,
            bannerType = "critical",
            blockApp = true,
            requiresAction = true
        )
        else -> FlagStatusConfig(
            status = "unknown",
            color = 0xFF9E9E9E,
            icon = "❓",
            title = "Unknown Status",
            message = "Please contact support.",
            showBanner = true,
            requiresAction = true
        )
    }
}

/**
 * Helper function to get severity color
 */
private fun getSeverityColor(severity: String): Color {
    return when (severity) {
        FlagConstants.SEVERITY_LOW -> Color(0xFF2196F3) // Blue
        FlagConstants.SEVERITY_MEDIUM -> Color(0xFFFF9800) // Orange
        FlagConstants.SEVERITY_HIGH -> Color(0xFFF44336) // Red
        FlagConstants.SEVERITY_CRITICAL -> Color(0xFFD32F2F) // Dark Red
        else -> Color(0xFF9E9E9E) // Gray
    }
}
