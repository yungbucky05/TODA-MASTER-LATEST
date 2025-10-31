package com.example.toda.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.data.*
import com.example.toda.viewmodel.ChatViewModel

@Composable
fun BookingChatIntegration(
    booking: Booking,
    currentUser: User,
    driver: User? = null,
    operator: User? = null,
    onOpenFullChat: () -> Unit,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val chatState by chatViewModel.chatState.collectAsStateWithLifecycle()
    val newMessageNotification by chatViewModel.newMessageNotification.collectAsStateWithLifecycle()

    // Initialize chat when component mounts
    LaunchedEffect(booking.id) {
        chatViewModel.setCurrentUser(currentUser)
        chatViewModel.setCurrentBooking(booking.id)
        chatViewModel.initializeChat(booking, currentUser, driver, operator)
    }

    Column(modifier = modifier) {
        // New message notification banner
        ChatNotificationBanner(
            newMessage = newMessageNotification,
            onDismiss = { chatViewModel.dismissNewMessageNotification() },
            onOpenChat = {
                chatViewModel.dismissNewMessageNotification()
                onOpenFullChat()
            }
        )

        // Chat card for quick access
        if (chatState.isConnected) {
            ChatCard(
                booking = booking,
                chatService = chatViewModel.chatService,
                currentUser = currentUser,
                onOpenChat = onOpenFullChat
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick chat actions
            QuickChatActions(
                bookingId = booking.id,
                chatService = chatViewModel.chatService,
                currentUser = currentUser
            )
        }
    }
}

@Composable
fun ChatFloatingActionButton(
    booking: Booking,
    currentUser: User,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    // Observe active chats for reactivity
    val activeChats by chatViewModel.activeChats.collectAsStateWithLifecycle()
    val hasActiveChat = remember(activeChats, booking.id) {
        activeChats.any { it.bookingId == booking.id && it.isActive }
    }

    LaunchedEffect(booking.id, booking.status) {
        chatViewModel.setCurrentUser(currentUser)
        chatViewModel.setCurrentBooking(booking.id)
        // Initialize chat once the booking is accepted or in progress and no active chat exists yet
        if ((booking.status == BookingStatus.ACCEPTED || booking.status == BookingStatus.IN_PROGRESS) && !hasActiveChat) {
            chatViewModel.initializeChat(booking, currentUser, null, null)
        }
    }

    // Only show the FAB when there's an active chat for this booking
    if (hasActiveChat) {
        ChatFloatingButton(
            bookingId = booking.id,
            chatService = chatViewModel.chatService,
            currentUser = currentUser,
            onClick = onOpenChat,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullChatScreen(
    bookingId: String,
    currentUser: User,
    onBack: () -> Unit,
    onLocationShare: () -> Unit = {},
    chatViewModel: ChatViewModel = hiltViewModel()
) {

    // Initialize chat
    LaunchedEffect(bookingId) {
        chatViewModel.setCurrentUser(currentUser)
        chatViewModel.setCurrentBooking(bookingId)
        chatViewModel.markMessagesAsRead()
    }

    ChatScreen(
        bookingId = bookingId,
        currentUser = currentUser,
        chatService = chatViewModel.chatService,
        onBack = onBack,
        onLocationShare = onLocationShare
    )
}

@Composable
fun ActiveBookingWithChat(
    booking: Booking,
    currentUser: User,
    driver: User? = null,
    operator: User? = null,
    onNavigateToFullChat: () -> Unit,
    onLocationShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFullChat by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Booking status card
        BookingStatusCard(booking = booking, currentUser = currentUser)

        Spacer(modifier = Modifier.height(16.dp))

        // Chat integration
        BookingChatIntegration(
            booking = booking,
            currentUser = currentUser,
            driver = driver,
            operator = operator,
            onOpenFullChat = { showFullChat = true }
        )

        // Driver-specific actions
        if (currentUser.userType == UserType.DRIVER) {
            Spacer(modifier = Modifier.height(16.dp))
            DriverChatActions(
                booking = booking,
                currentUser = currentUser
            )
        }
    }

    // Full chat overlay
    if (showFullChat) {
        FullChatScreen(
            bookingId = booking.id,
            currentUser = currentUser,
            onBack = { showFullChat = false },
            onLocationShare = onLocationShare
        )
    }
}

@Composable
private fun BookingStatusCard(
    booking: Booking,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (booking.status) {
                BookingStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                BookingStatus.ACCEPTED -> MaterialTheme.colorScheme.primaryContainer
                BookingStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                BookingStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Booking #${booking.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium
                )
                AssistChip(
                    onClick = { },
                    label = { Text(booking.status.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "From: ${booking.pickupLocation}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "To: ${booking.destination}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (booking.driverName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Driver: ${booking.driverName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverChatActions(
    booking: Booking,
    currentUser: User,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Driver Actions",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { chatViewModel.sendDriverArrivalNotification() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.NotificationImportant, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Arrived")
                }

                OutlinedButton(
                    onClick = { chatViewModel.sendLocationUpdate("Current location shared") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Location")
                }
            }
        }
    }
}
