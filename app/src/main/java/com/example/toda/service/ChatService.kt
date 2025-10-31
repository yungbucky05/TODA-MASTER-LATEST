package com.example.toda.service

import com.example.toda.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class ChatService {

    // Mock database for chat messages
    private val chatMessages = mutableMapOf<String, MutableList<ChatMessage>>()
    private val activeChats = mutableMapOf<String, ActiveChat>()

    // StateFlows for real-time updates
    private val _chatStates = mutableMapOf<String, MutableStateFlow<ChatState>>()
    private val _activeChats = MutableStateFlow<List<ActiveChat>>(emptyList())

    val activeChatsFlow: StateFlow<List<ActiveChat>> = _activeChats.asStateFlow()

    // Initialize chat for a booking
    fun initializeChatForBooking(
        booking: Booking,
        customer: User,
        driver: User?,
        operator: User?
    ): String {
        val participants = mutableListOf<ChatParticipant>()

        // Add customer
        participants.add(
            ChatParticipant(
                userId = customer.id,
                name = customer.name,
                userType = UserType.PASSENGER,
                isOnline = true
            )
        )

        // Add driver if assigned
        driver?.let {
            participants.add(
                ChatParticipant(
                    userId = it.id,
                    name = it.name,
                    userType = UserType.DRIVER,
                    isOnline = true
                )
            )
        }

        // Add operator if involved
        operator?.let {
            participants.add(
                ChatParticipant(
                    userId = it.id,
                    name = it.name,
                    userType = UserType.OPERATOR,
                    isOnline = true
                )
            )
        }

        val chatId = booking.id
        val activeChat = ActiveChat(
            bookingId = chatId,
            participants = participants,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )

        activeChats[chatId] = activeChat
        chatMessages[chatId] = mutableListOf()
        _chatStates[chatId] = MutableStateFlow(
            ChatState(
                messages = chatMessages[chatId] ?: emptyList(),
                isConnected = true
            )
        )

        // Send initial system message
        val systemMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            bookingId = chatId,
            senderId = "system",
            senderName = "TODA System",
            receiverId = "all",
            message = "Chat started for booking ${booking.id}. Driver: ${driver?.name ?: "Pending"}, Customer: ${customer.name}",
            timestamp = System.currentTimeMillis(),
            messageType = "SYSTEM"
        )

        sendMessage(systemMessage)
        updateActiveChats()

        return chatId
    }

    // Send a message
    fun sendMessage(message: ChatMessage) {
        val bookingId = message.bookingId

        // Add message to chat
        chatMessages.getOrPut(bookingId) { mutableListOf() }.add(message)

        // Update active chat
        activeChats[bookingId]?.let { chat ->
            activeChats[bookingId] = chat.copy(
                lastMessage = message,
                unreadCount = chat.unreadCount + 1
            )
        }

        // Update chat state
        _chatStates[bookingId]?.let { stateFlow ->
            val currentState = stateFlow.value
            stateFlow.value = currentState.copy(
                messages = chatMessages[bookingId] ?: emptyList(),
                lastMessageTime = message.timestamp,
                unreadCount = currentState.unreadCount + 1
            )
        }

        updateActiveChats()
    }

    // Get chat state for a booking
    fun getChatState(bookingId: String): StateFlow<ChatState> {
        return _chatStates.getOrPut(bookingId) {
            MutableStateFlow(
                ChatState(
                    messages = chatMessages[bookingId] ?: emptyList(),
                    isConnected = activeChats[bookingId]?.isActive == true
                )
            )
        }.asStateFlow()
    }

    // Get messages for a booking
    fun getMessages(bookingId: String): List<ChatMessage> {
        return chatMessages[bookingId] ?: emptyList()
    }

    // Mark messages as read
    fun markMessagesAsRead(bookingId: String, userId: String) {
        chatMessages[bookingId]?.forEach { message ->
            if (message.receiverId == userId || message.receiverId == "all") {
                // In a real implementation, you'd update the message read status
            }
        }

        // Update unread count
        _chatStates[bookingId]?.let { stateFlow ->
            val currentState = stateFlow.value
            stateFlow.value = currentState.copy(unreadCount = 0)
        }

        activeChats[bookingId]?.let { chat ->
            activeChats[bookingId] = chat.copy(unreadCount = 0)
        }

        updateActiveChats()
    }

    // Update participant online status
    fun updateParticipantStatus(bookingId: String, userId: String, isOnline: Boolean) {
        activeChats[bookingId]?.let { chat ->
            val updatedParticipants = chat.participants.map { participant ->
                if (participant.userId == userId) {
                    participant.copy(
                        isOnline = isOnline,
                        lastSeen = if (!isOnline) System.currentTimeMillis() else participant.lastSeen
                    )
                } else {
                    participant
                }
            }

            activeChats[bookingId] = chat.copy(participants = updatedParticipants)
            updateActiveChats()
        }
    }

    // End chat when booking is completed
    fun endChat(bookingId: String) {
        activeChats[bookingId]?.let { chat ->
            activeChats[bookingId] = chat.copy(isActive = false)

            // Set connection state to false before sending system message
            _chatStates[bookingId]?.let { stateFlow ->
                val currentState = stateFlow.value
                stateFlow.value = currentState.copy(isConnected = false)
            }

            // Send system message
            val systemMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                bookingId = bookingId,
                senderId = "system",
                senderName = "TODA System",
                receiverId = "all",
                message = "Trip completed. Chat ended.",
                timestamp = System.currentTimeMillis(),
                messageType = "SYSTEM"
            )

            sendMessage(systemMessage)
        }
    }

    // Get active chat for a booking
    fun getActiveChat(bookingId: String): ActiveChat? {
        return activeChats[bookingId]
    }

    // Private helper to update active chats flow
    private fun updateActiveChats() {
        _activeChats.value = activeChats.values.filter { it.isActive }.sortedByDescending {
            it.lastMessage?.timestamp ?: it.createdAt
        }
    }

    // Emergency chat functionality
    fun createEmergencyChat(userId: String, userName: String, message: String): String {
        val chatId = "emergency_${System.currentTimeMillis()}"

        val emergencyMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            bookingId = chatId,
            senderId = userId,
            senderName = userName,
            receiverId = "emergency_operator",
            message = "EMERGENCY: $message",
            timestamp = System.currentTimeMillis(),
            messageType = "EMERGENCY"
        )

        chatMessages[chatId] = mutableListOf(emergencyMessage)
        _chatStates[chatId] = MutableStateFlow(ChatState(
            messages = listOf(emergencyMessage),
            isConnected = true
        ))

        return chatId
    }

    // Get chat history for a user
    fun getUserChatHistory(userId: String): List<ActiveChat> {
        return activeChats.values.filter { chat ->
            chat.participants.any { participant -> participant.userId == userId }
        }.sortedByDescending { it.lastMessage?.timestamp ?: it.createdAt }
    }

    // Missing methods that are referenced in the compilation errors
    fun getChatStateFlow(bookingId: String): StateFlow<ChatState> {
        return getChatState(bookingId)
    }

    fun setTypingStatus(bookingId: String, userId: String, isTyping: Boolean) {
        _chatStates[bookingId]?.let { stateFlow ->
            val currentState = stateFlow.value
            stateFlow.value = currentState.copy(
                isTyping = isTyping,
                typingUser = if (isTyping) userId else null
            )
        }
    }

    fun sendLocationUpdate(bookingId: String, senderId: String, senderName: String, latitude: Double, longitude: Double) {
        val locationMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            bookingId = bookingId,
            senderId = senderId,
            senderName = senderName,
            receiverId = "all",
            message = "Location: $latitude, $longitude",
            timestamp = System.currentTimeMillis(),
            messageType = "LOCATION"
        )
        sendMessage(locationMessage)
    }

    fun getUnreadCount(bookingId: String): Int {
        return _chatStates[bookingId]?.value?.unreadCount ?: 0
    }

    fun isParticipant(bookingId: String, userId: String): Boolean {
        return activeChats[bookingId]?.participants?.any { it.userId == userId } ?: false
    }
}
