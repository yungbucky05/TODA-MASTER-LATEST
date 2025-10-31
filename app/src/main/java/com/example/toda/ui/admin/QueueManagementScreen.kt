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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.data.QueueEntry
import com.example.toda.viewmodel.EnhancedBookingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueManagementScreen(
    onBack: () -> Unit,
    viewModel: EnhancedBookingViewModel = hiltViewModel()
) {
    val queueList by viewModel.queueList.collectAsStateWithLifecycle()
    var showRemoveDialog by remember { mutableStateOf(false) }
    var selectedQueueEntry by remember { mutableStateOf<QueueEntry?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
                text = "Driver Queue Management",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Queue Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Drivers in Queue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${queueList.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Queue List
        if (queueList.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No drivers in queue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Drivers will appear here when they join the queue",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(queueList) { queueEntry ->
                    QueueEntryCard(
                        queueEntry = queueEntry,
                        position = queueList.indexOf(queueEntry) + 1,
                        onRemove = {
                            selectedQueueEntry = queueEntry
                            showRemoveDialog = true
                        }
                    )
                }
            }
        }
    }

    // Remove confirmation dialog
    if (showRemoveDialog && selectedQueueEntry != null) {
        AlertDialog(
            onDismissRequest = {
                showRemoveDialog = false
                selectedQueueEntry = null
            },
            title = { Text("Remove from Queue") },
            text = {
                Text("Are you sure you want to remove ${selectedQueueEntry?.driverName ?: "this driver"} from the queue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Store the entry reference before clearing state
                        val entryToRemove = selectedQueueEntry

                        // Close dialog immediately
                        showRemoveDialog = false
                        selectedQueueEntry = null

                        // Then perform the removal in a coroutine
                        entryToRemove?.let { entry ->
                            coroutineScope.launch {
                                val result = viewModel.leaveQueue(entry.driverRFID)
                                result.fold(
                                    onSuccess = { success ->
                                        if (success) {
                                            println("✓ Successfully removed driver from queue")
                                        } else {
                                            println("✗ Failed to remove driver from queue")
                                        }
                                    },
                                    onFailure = { error ->
                                        println("✗ Error removing driver: ${error.message}")
                                    }
                                )
                            }
                        }
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        selectedQueueEntry = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QueueEntryCard(
    queueEntry: QueueEntry,
    position: Int,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position Badge
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "#$position",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = queueEntry.driverName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "RFID: ${queueEntry.driverRFID}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Joined: ${formatTimestamp(queueEntry.timestamp)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Remove Button
            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from queue"
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
