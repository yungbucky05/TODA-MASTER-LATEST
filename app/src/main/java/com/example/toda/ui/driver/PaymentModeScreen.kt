package com.example.toda.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentModeScreen(
    driverId: String,
    driverName: String = "",
    currentPaymentMode: String? = null,
    currentBalance: Double = 0.0,
    onModeSelected: (String) -> Unit,
    onPayBalance: (() -> Unit)? = null,
    isFirstTimeSetup: Boolean = false
) {
    var showModeDialog by remember { mutableStateOf(isFirstTimeSetup) }

    // If it's first-time setup and no payment mode, show full-screen selection
    if (isFirstTimeSetup && currentPaymentMode == null) {
        PaymentModeSelectionScreen(
            driverName = driverName,
            onModeSelected = onModeSelected
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Settings") },
                navigationIcon = {
                    if (!isFirstTimeSetup) {
                        IconButton(onClick = { /* onBack handled by caller */ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val paymentModeLabel = when (currentPaymentMode) {
                "pay_later" -> "Pay Later"
                "pay_balance" -> "Pay Balance Now"
                "pay_every_trip" -> "Pay Every Trip"
                null -> "Not Set"
                else -> currentPaymentMode.replaceFirstChar { it.uppercase() }
            }

            // Current Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentBalance > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Balance",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₱%.2f".format(currentBalance),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBalance > 0) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                    )

                    if (currentBalance > 0 && onPayBalance != null && currentPaymentMode != "pay_balance") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onPayBalance,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay Balance")
                        }
                    }
                }
            }

            // Payment Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = paymentModeLabel,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { showModeDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF9C4)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF57C00)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Payment Mode Information",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Pay Every Trip: Pay ₱5 before each trip\n" +
                                   "• Pay Later: Accumulate ₱5 per trip, pay at end of day\n" +
                                   "• Pay Balance Now: Temporarily switch to settle outstanding balance at the terminal\n" +
                                   "• You must settle your balance before going online the next day",
                            fontSize = 12.sp,
                            color = Color(0xFF6D4C41)
                        )
                    }
                }
            }
        }
    }

    // Payment Mode Selection Dialog
    if (showModeDialog && !isFirstTimeSetup) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("Select Payment Mode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentModeOption(
                        title = "Pay Every Trip",
                        description = "Pay ₱5.00 contribution before each trip",
                        isSelected = currentPaymentMode == "pay_every_trip",
                        onClick = {
                            onModeSelected("pay_every_trip")
                            showModeDialog = false
                        }
                    )

                    PaymentModeOption(
                        title = "Pay Later",
                        description = "Accumulate ₱5.00 per trip, pay before next day",
                        isSelected = currentPaymentMode == "pay_later",
                        onClick = {
                            onModeSelected("pay_later")
                            showModeDialog = false
                        }
                    )

                    PaymentModeOption(
                        title = "Pay Balance Now",
                        description = "Switch kiosk to balance payoff mode (requires outstanding balance)",
                        isSelected = currentPaymentMode == "pay_balance",
                        enabled = currentBalance > 0,
                        onClick = {
                            onModeSelected("pay_balance")
                            showModeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                !enabled -> Color(0xFFF0F0F0)
                isSelected -> Color(0xFFE3F2FD)
                else -> Color(0xFFF5F5F5)
            }
        )
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
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = if (enabled) Color.Unspecified else Color.Gray
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (enabled) Color.Gray else Color.LightGray
                )
            }
        }
    }
}
