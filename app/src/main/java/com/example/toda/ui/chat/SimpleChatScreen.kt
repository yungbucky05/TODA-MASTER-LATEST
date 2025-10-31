package com.example.toda.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.data.User
import com.example.toda.data.Booking
import com.example.toda.viewmodel.EnhancedBookingViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.net.toUri
import com.example.toda.data.UserType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatScreen(
    user: User,
    booking: Booking,
    onBack: () -> Unit,
    viewModel: EnhancedBookingViewModel = hiltViewModel()
) {
    val chatMessages by viewModel.getChatMessages(booking.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Auto-create chat room when screen opens
    LaunchedEffect(booking.id) {
        println("SimpleChatScreen: Auto-creating chat room for booking ${booking.id}")
        if (booking.assignedDriverId.isNotEmpty()) {
            try {
                // Get driver details from booking data or fetch from database
                val driverName = booking.driverName.ifEmpty { "Driver" }

                // Use the viewModel's suspend function instead of accessing repository directly
                val result = viewModel.createOrGetChatRoom(
                    bookingId = booking.id,
                    customerId = booking.customerId,
                    customerName = booking.customerName,
                    driverId = booking.assignedDriverId,
                    driverName = driverName
                )
                result.fold(
                    onSuccess = { chatRoomId ->
                        println("✅ Chat room created/found successfully: $chatRoomId")
                    },
                    onFailure = { error ->
                        println("❌ Failed to create/find chat room: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error in chat room creation: ${e.message}")
            }
        } else {
            println("⚠️ No assigned driver for booking ${booking.id}")
        }
    }

    // Debug chat messages loading
    LaunchedEffect(chatMessages.size) {
        println("📱 SimpleChatScreen: Chat messages count changed to ${chatMessages.size}")
        chatMessages.forEachIndexed { index, message ->
            println("   Message $index: ${message.senderName}: ${message.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Chat Header with optional Call button for drivers
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (user.id == booking.customerId) {
                            "Chat with ${booking.driverName.ifEmpty { "Driver" }}"
                        } else {
                            "Chat with ${booking.customerName}"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Booking: ${booking.id.take(8)}...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Show call button if driver and phone number is available
                if (user.userType == UserType.DRIVER && booking.phoneNumber.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val sanitized = booking.phoneNumber.filter { it.isDigit() || it == '+' }
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = "tel:$sanitized".toUri()
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call customer"
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start your conversation here",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(chatMessages) { message ->
                    ChatMessageBubble(
                        message = message,
                        isOwnMessage = message.senderId == user.id,
                        isSystemMessage = message.messageType == "SYSTEM"
                    )
                }
            }
        }

        // Message Input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val receiverId = if (user.id == booking.customerId) {
                                booking.assignedDriverId
                            } else {
                                booking.customerId
                            }

                            println("📤 Sending message: '${messageText.trim()}'")
                            println("   From: ${user.name} (${user.id})")
                            println("   To: $receiverId")
                            println("   Booking: ${booking.id}")

                            // Send message using viewModel
                            viewModel.sendMessage(
                                bookingId = booking.id,
                                senderId = user.id,
                                senderName = user.name,
                                receiverId = receiverId,
                                message = messageText.trim()
                            )
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
