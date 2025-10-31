package com.example.toda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.FirebaseBooking
import com.google.firebase.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingsViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val bookingsRef = database.getReference("bookings")

    private val _bookings = MutableStateFlow<List<FirebaseBooking>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<String?>(null)
    private val _selectedDate = MutableStateFlow<Date?>(null)

    val searchQuery = _searchQuery.asStateFlow()
    val selectedStatus = _selectedStatus.asStateFlow()
    val selectedDate = _selectedDate.asStateFlow()

    // Filtered bookings
    val filteredBookings = combine(
        _bookings,
        _searchQuery,
        _selectedStatus,
        _selectedDate
    ) { bookings, query, status, date ->
        var result = bookings

        // Apply search query filter
        if (query.isNotEmpty()) {
            result = result.filter { booking ->
                booking.customerName.contains(query, ignoreCase = true) ||
                booking.driverName.contains(query, ignoreCase = true) ||
                booking.pickupLocation.contains(query, ignoreCase = true) ||
                booking.destination.contains(query, ignoreCase = true)
            }
        }

        // Apply status filter
        if (!status.isNullOrEmpty()) {
            result = result.filter { it.status == status }
        }

        // Apply date filter
        if (date != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = sdf.format(date)
            result = result.filter {
                val bookingDate = sdf.format(Date(it.timestamp))
                bookingDate == dateStr
            }
        }

        result.sortedByDescending { it.timestamp }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        // Listen for bookings changes
        bookingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookingsList = mutableListOf<FirebaseBooking>()
                for (bookingSnapshot in snapshot.children) {
                    try {
                        val booking = bookingSnapshot.getValue(FirebaseBooking::class.java)
                        booking?.let { bookingsList.add(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _bookings.value = bookingsList
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateStatusFilter(status: String?) {
        _selectedStatus.value = status
    }

    fun updateDateFilter(date: Date?) {
        _selectedDate.value = date
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedStatus.value = null
        _selectedDate.value = null
    }

    companion object {
        val BOOKING_STATUSES = listOf(
            "PENDING",
            "ACCEPTED",
            "IN_PROGRESS",
            "COMPLETED",
            "CANCELLED"
        )
    }
}
