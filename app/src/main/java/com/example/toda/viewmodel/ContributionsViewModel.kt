package com.example.toda.viewmodel

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toda.data.FirebaseContribution
import com.google.firebase.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DriverStats(
    val driverId: String,
    val driverName: String,
    val todayTotal: Double,
    val weekTotal: Double,
    val monthTotal: Double,
    val totalContributions: Int
)

class ContributionsViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val contributionsRef = database.getReference("contributions")

    private val _contributions = MutableStateFlow<List<FirebaseContribution>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedDate = MutableStateFlow<Date?>(null)
    private val _selectedDriver = MutableStateFlow<String?>(null) // Now stores driver NAME, not RFID

    val searchQuery = _searchQuery.asStateFlow()
    val selectedDate = _selectedDate.asStateFlow()
    val selectedDriver = _selectedDriver.asStateFlow()

    private val _todayTotal = MutableStateFlow(0.0)
    val todayTotal = _todayTotal.asStateFlow()

    private val _monthTotal = MutableStateFlow(0.0)
    val monthTotal = _monthTotal.asStateFlow()

    private val _weekTotal = MutableStateFlow(0.0)
    val weekTotal = _weekTotal.asStateFlow()

    // Driver-specific stats
    private val _driverStats = MutableStateFlow<DriverStats?>(null)
    val driverStats = _driverStats.asStateFlow()

    // Available drivers for dropdown - use driverName as the unique identifier for search
    val availableDrivers = _contributions.map { contributions ->
        println("=== Building availableDrivers ===")
        println("Total contributions: ${contributions.size}")

        val drivers = contributions
            .filter { it.driverName.isNotBlank() }
            .distinctBy { it.driverName } // Use driver NAME as unique identifier, not RFID
            .sortedBy { it.driverName }

        println("Available drivers count = ${drivers.size}")
        drivers.forEach { driver ->
            println("  - ${driver.driverName} (RFID: ${driver.driverId})")
        }

        drivers
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Filtered contributions
    val filteredContributions = combine(
        _contributions,
        _searchQuery,
        _selectedDate,
        _selectedDriver
    ) { contributions, query, date, driverName ->
        var result = contributions

        // Apply driver filter - now uses driver NAME instead of RFID
        if (driverName != null) {
            result = result.filter { it.driverName == driverName }
        }

        // Apply search query filter
        if (query.isNotEmpty()) {
            result = result.filter { contribution ->
                contribution.driverName.contains(query, ignoreCase = true) ||
                contribution.driverId.contains(query, ignoreCase = true)
            }
        }

        // Apply date filter
        if (date != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = sdf.format(date)
            result = result.filter {
                val contributionDate = sdf.format(Date(it.timestamp * 1000))
                contributionDate == dateStr
            }
        }

        result.sortedByDescending { it.timestamp }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        contributionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    val contributionsList = mutableListOf<FirebaseContribution>()
                    for (contributionSnapshot in snapshot.children) {
                        try {
                            val timestamp = contributionSnapshot.child("timestamp").getValue(String::class.java) ?: ""
                            val amount = contributionSnapshot.child("amount").getValue(Long::class.java)?.toDouble() ?: 0.0
                            val driverName = contributionSnapshot.child("driverName").getValue(String::class.java) ?: ""
                            val driverRFID = contributionSnapshot.child("driverRFID").getValue(String::class.java) ?: ""

                            val contribution = FirebaseContribution(
                                driverId = driverRFID, // Keep for backward compatibility
                                driverRFID = driverRFID, // Use the actual field name
                                driverName = driverName,
                                amount = amount,
                                timestamp = timestamp.toLongOrNull() ?: 0L,
                                source = "hardware"
                            )
                            contributionsList.add(contribution)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    _contributions.value = contributionsList
                    updateTotals(contributionsList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })

        // Update driver stats when selected driver changes
        viewModelScope.launch {
            combine(_contributions, _selectedDriver) { contributions, driverName ->
                if (driverName != null) {
                    calculateDriverStats(contributions, driverName)
                } else {
                    null
                }
            }.collect { stats ->
                _driverStats.value = stats
            }
        }
    }

    private fun updateTotals(contributions: List<FirebaseContribution>) {
        val calendar = Calendar.getInstance()

        // Get start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis / 1000

        // Get start of week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = calendar.timeInMillis / 1000

        // Get start of month
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Reset
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val startOfMonth = calendar.timeInMillis / 1000

        val todayTotal = contributions
            .filter { it.timestamp >= startOfDay }
            .sumOf { it.amount }

        val weekTotal = contributions
            .filter { it.timestamp >= startOfWeek }
            .sumOf { it.amount }

        val monthTotal = contributions
            .filter { it.timestamp >= startOfMonth }
            .sumOf { it.amount }

        _todayTotal.value = todayTotal
        _weekTotal.value = weekTotal
        _monthTotal.value = monthTotal
    }

    private fun calculateDriverStats(contributions: List<FirebaseContribution>, driverName: String): DriverStats? {
        val driverContributions = contributions.filter { it.driverName == driverName }
        if (driverContributions.isEmpty()) return null

        val calendar = Calendar.getInstance()

        // Get start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis / 1000

        // Get start of week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = calendar.timeInMillis / 1000

        // Get start of month
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val startOfMonth = calendar.timeInMillis / 1000

        return DriverStats(
            driverId = driverContributions.first().driverId,
            driverName = driverName,
            todayTotal = driverContributions.filter { it.timestamp >= startOfDay }.sumOf { it.amount },
            weekTotal = driverContributions.filter { it.timestamp >= startOfWeek }.sumOf { it.amount },
            monthTotal = driverContributions.filter { it.timestamp >= startOfMonth }.sumOf { it.amount },
            totalContributions = driverContributions.size
        )
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateDateFilter(date: Date?) {
        _selectedDate.value = date
    }

    fun updateDriverFilter(driverId: String?) {
        _selectedDriver.value = driverId
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedDate.value = null
        _selectedDriver.value = null
    }

    fun getFormattedAmount(amount: Double): String {
        return "₱%.2f".format(Locale.US, amount)
    }

    fun downloadDriverReport(context: Context, driverName: String): Boolean {
        val driverContributions = _contributions.value.filter { it.driverName == driverName }
        if (driverContributions.isEmpty()) {
            Toast.makeText(context, "No contributions found for this driver", Toast.LENGTH_SHORT).show()
            return false
        }

        val stats = _driverStats.value
        if (stats == null) {
            Toast.makeText(context, "Unable to generate report", Toast.LENGTH_SHORT).show()
            return false
        }

        val txtContent = generateDriverReportTXT(stats, driverContributions)

        return try {
            val fileName = "driver_report_${stats.driverName.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt"

            // Save to Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            file.writeText(txtContent)

            Toast.makeText(context, "Report downloaded to Downloads folder:\n$fileName", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error downloading report: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun generateDriverReportTXT(stats: DriverStats, contributions: List<FirebaseContribution>): String {
        val sb = StringBuilder()

        sb.appendLine("═══════════════════════════════════════════════════════")
        sb.appendLine("    TODA CONTRIBUTION SYSTEM - DRIVER REPORT")
        sb.appendLine("═══════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("Generated: ${SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.US).format(Date())}")
        sb.appendLine()
        sb.appendLine("───────────────────────────────────────────────────────")
        sb.appendLine("DRIVER INFORMATION")
        sb.appendLine("───────────────────────────────────────────────────────")
        sb.appendLine("Name:          ${stats.driverName}")
        sb.appendLine("RFID:          ${stats.driverId}")
        sb.appendLine("Total Trips:   ${stats.totalContributions}")
        sb.appendLine()
        sb.appendLine("───────────────────────────────────────────────────────")
        sb.appendLine("SUMMARY STATISTICS")
        sb.appendLine("───────────────────────────────────────────────────────")
        sb.appendLine("Today's Total:      ${getFormattedAmount(stats.todayTotal)}")
        sb.appendLine("This Week's Total:  ${getFormattedAmount(stats.weekTotal)}")
        sb.appendLine("This Month's Total: ${getFormattedAmount(stats.monthTotal)}")
        sb.appendLine()
        sb.appendLine("───────────────────────────────────────────────────────")
        sb.appendLine("DETAILED TRANSACTION HISTORY")
        sb.appendLine("───────────────────────────────────────────────────────")
        sb.appendLine()

        // Transaction details
        contributions.sortedByDescending { it.timestamp }.forEach { contribution ->
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(contribution.timestamp * 1000))
            val time = SimpleDateFormat("hh:mm a", Locale.US).format(Date(contribution.timestamp * 1000))
            val amount = getFormattedAmount(contribution.amount)
            val source = contribution.source.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }

            sb.appendLine("Date:   $date")
            sb.appendLine("Time:   $time")
            sb.appendLine("Amount: $amount")
            sb.appendLine("Source: $source")
            sb.appendLine("---")
        }

        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════════════════════")
        sb.appendLine("              END OF REPORT")
        sb.appendLine("═══════════════════════════════════════════════════════")

        return sb.toString()
    }
}
