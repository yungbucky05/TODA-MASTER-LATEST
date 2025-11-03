package com.example.toda.data

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Data models for Flagged Accounts System
 * Based on MOBILE_APP_INTEGRATION.md specification
 */

/**
 * Enum representing flag status levels
 */
enum class FlagStatus {
    GOOD,
    MONITORED,
    RESTRICTED,
    SUSPENDED;
    
    companion object {
        fun fromString(status: String): FlagStatus {
            return when (status.lowercase()) {
                "good" -> GOOD
                "monitored" -> MONITORED
                "restricted" -> RESTRICTED
                "suspended" -> SUSPENDED
                else -> GOOD
            }
        }
    }
    
    fun toDisplayString(): String {
        return when (this) {
            GOOD -> "Good Standing"
            MONITORED -> "Monitored"
            RESTRICTED -> "Restricted"
            SUSPENDED -> "Suspended"
        }
    }
}

@IgnoreExtraProperties
data class DriverFlagData(
    val flagScore: Int = 0,
    val flagStatus: String = "good" // "good" | "monitored" | "restricted" | "suspended"
)

@IgnoreExtraProperties
data class DriverFlag(
    val flagId: String = "",
    val type: String = "",
    val severity: String = "", // "low" | "medium" | "high" | "critical"
    val points: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "active", // "active" | "resolved" | "dismissed"
    val details: Map<String, Any> = emptyMap(),
    val notes: String = ""
)

/**
 * Flag status configuration for UI display
 */
data class FlagStatusConfig(
    val status: String,
    val color: Long,
    val icon: String,
    val title: String,
    val message: String,
    val showBanner: Boolean = false,
    val bannerType: String = "info",
    val blockApp: Boolean = false,
    val requiresAction: Boolean = false
)

/**
 * Flag type information for display
 */
data class FlagTypeInfo(
    val type: String,
    val displayName: String,
    val icon: String,
    val message: String,
    val actionableMessage: String
)

/**
 * Helper object for flag-related constants and utilities
 */
object FlagConstants {
    // Status thresholds
    const val GOOD_MAX = 50
    const val MONITORED_MAX = 150
    const val RESTRICTED_MAX = 300
    
    // Status values
    const val STATUS_GOOD = "good"
    const val STATUS_MONITORED = "monitored"
    const val STATUS_RESTRICTED = "restricted"
    const val STATUS_SUSPENDED = "suspended"
    
    // Flag types
    const val LOW_CONTRIBUTIONS = "LOW_CONTRIBUTIONS"
    const val INACTIVE_ACCOUNT = "INACTIVE_ACCOUNT"
    const val HIGH_CANCELLATION_RATE = "HIGH_CANCELLATION_RATE"
    const val CUSTOMER_COMPLAINTS = "CUSTOMER_COMPLAINTS"
    const val RFID_ISSUES = "RFID_ISSUES"
    
    // Flag severity
    const val SEVERITY_LOW = "low"
    const val SEVERITY_MEDIUM = "medium"
    const val SEVERITY_HIGH = "high"
    const val SEVERITY_CRITICAL = "critical"
    
    // Flag status
    const val FLAG_STATUS_ACTIVE = "active"
    const val FLAG_STATUS_RESOLVED = "resolved"
    const val FLAG_STATUS_DISMISSED = "dismissed"
    
    /**
     * Get flag status based on score
     */
    fun getStatusFromScore(score: Int): String {
        return when {
            score > RESTRICTED_MAX -> STATUS_SUSPENDED
            score > MONITORED_MAX -> STATUS_RESTRICTED
            score > GOOD_MAX -> STATUS_MONITORED
            else -> STATUS_GOOD
        }
    }
    
    /**
     * Get flag type display info
     */
    fun getFlagTypeInfo(type: String): FlagTypeInfo {
        return when (type) {
            LOW_CONTRIBUTIONS -> FlagTypeInfo(
                type = type,
                displayName = "Low Contributions",
                icon = "💰",
                message = "Your contribution amount is below average",
                actionableMessage = "Please increase your weekly contributions to maintain good standing"
            )
            INACTIVE_ACCOUNT -> FlagTypeInfo(
                type = type,
                displayName = "Inactive Account",
                icon = "😴",
                message = "You haven't logged in recently",
                actionableMessage = "Log in regularly to maintain good standing"
            )
            HIGH_CANCELLATION_RATE -> FlagTypeInfo(
                type = type,
                displayName = "High Cancellation Rate",
                icon = "🚫",
                message = "You're cancelling too many bookings",
                actionableMessage = "Avoid cancelling accepted bookings to improve your status"
            )
            CUSTOMER_COMPLAINTS -> FlagTypeInfo(
                type = type,
                displayName = "Customer Complaints",
                icon = "😠",
                message = "Multiple customer complaints detected",
                actionableMessage = "Improve service quality to resolve this flag"
            )
            RFID_ISSUES -> FlagTypeInfo(
                type = type,
                displayName = "RFID Issues",
                icon = "🔑",
                message = "Problems with RFID card usage detected",
                actionableMessage = "Ensure proper RFID card usage"
            )
            else -> FlagTypeInfo(
                type = type,
                displayName = type.replace("_", " "),
                icon = "⚠️",
                message = "Issue detected with your account",
                actionableMessage = "Please contact support to resolve this issue"
            )
        }
    }
}
