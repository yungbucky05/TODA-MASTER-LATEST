package com.example.toda.service

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverPaymentService @Inject constructor(
    private val database: FirebaseDatabase
) {
    companion object {
        private const val TRIP_CONTRIBUTION = 5.0
        private const val DRIVERS_PATH = "drivers"
        private const val PAYMENT_HISTORY_PATH = "payment_history"
    }

    /**
     * Record trip contribution based on payment mode
     *
     * Pay Every Trip:
     * - P5 was already paid BEFORE the trip (during queue join via hardware)
     * - This just records the contribution in history for tracking
     * - Balance stays at 0
     *
     * Pay Later:
     * - P5 is added to balance AFTER trip completion
     * - Balance accumulates throughout the day
     *
     * NOTE: Online/offline status is managed entirely by hardware system.
     * This function only tracks balance and payment history.
     */
    suspend fun recordTripContribution(driverId: String, bookingId: String): Result<Unit> {
        return try {
            val driverRef = database.getReference("$DRIVERS_PATH/$driverId")
            val snapshot = driverRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Driver not found"))
            }

            val paymentMode = snapshot.child("paymentMode").getValue(String::class.java) ?: "pay_every_trip"
            val currentBalance = snapshot.child("balance").getValue(Double::class.java) ?: 0.0

            if (paymentMode == "pay_later") {
                // Pay Later: Add P5 to balance AFTER trip completion
                val newBalance = currentBalance + TRIP_CONTRIBUTION
                val updates = hashMapOf<String, Any>(
                    "balance" to newBalance,
                    "lastPaymentDate" to System.currentTimeMillis()
                )
                driverRef.updateChildren(updates).await()

                println("✓ [Pay Later] Balance updated: ₱$currentBalance → ₱$newBalance for driver $driverId")

                // Record in payment history
                recordPaymentHistory(driverId, bookingId, TRIP_CONTRIBUTION, "TRIP_CONTRIBUTION")
            } else {
                // Pay Every Trip: P5 was already paid upfront (before joining queue)
                // Just record in history for tracking, no balance change
                if (!snapshot.hasChild("balance")) {
                    driverRef.child("balance").setValue(0.0).await()
                    println("✓ Initialized balance field to 0.0 for driver $driverId")
                }
                println("✓ [Pay Every Trip] Contribution recorded for trip $bookingId (P5 was paid upfront)")
                recordPaymentHistory(driverId, bookingId, TRIP_CONTRIBUTION, "TRIP_CONTRIBUTION_PREPAID")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process driver payment to clear balance
     * NOTE: This is for manual/hardware payments. App only displays balance.
     */
    suspend fun processPayment(driverId: String, amount: Double): Result<Unit> {
        return try {
            val driverRef = database.getReference("$DRIVERS_PATH/$driverId")
            val snapshot = driverRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Driver not found"))
            }

            val currentBalance = snapshot.child("balance").getValue(Double::class.java) ?: 0.0
            val newBalance = maxOf(0.0, currentBalance - amount)

            val updates = hashMapOf<String, Any>(
                "balance" to newBalance,
                "lastPaymentDate" to System.currentTimeMillis()
            )
            driverRef.updateChildren(updates).await()

            // Record payment in history
            recordPaymentHistory(driverId, null, amount, "PAYMENT")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get driver's current balance
     */
    suspend fun getDriverBalance(driverId: String): Result<Double> {
        return try {
            val driverRef = database.getReference("$DRIVERS_PATH/$driverId")
            val snapshot = driverRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Driver not found"))
            }

            val balance = snapshot.child("balance").getValue(Double::class.java) ?: 0.0
            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update driver's payment mode
     */
    suspend fun updatePaymentMode(driverId: String, paymentMode: String): Result<Unit> {
        return try {
            println("=== UPDATING PAYMENT MODE ===")
            println("Driver ID: $driverId")
            println("New Payment Mode: $paymentMode")

            if (paymentMode != "pay_every_trip" && paymentMode != "pay_later") {
                println("✗ Invalid payment mode: $paymentMode")
                return Result.failure(Exception("Invalid payment mode"))
            }

            val driverRef = database.getReference("$DRIVERS_PATH/$driverId")
            driverRef.child("paymentMode").setValue(paymentMode).await()

            println("✓ Payment mode updated successfully in Firebase")
            println("   Path: $DRIVERS_PATH/$driverId/paymentMode")
            println("   Value: $paymentMode")

            Result.success(Unit)
        } catch (e: Exception) {
            println("✗ Failed to update payment mode: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get payment history for a driver
     */
    suspend fun getPaymentHistory(driverId: String): Result<List<PaymentHistoryEntry>> {
        return try {
            val historyRef = database.getReference(PAYMENT_HISTORY_PATH)
            val snapshot = historyRef.orderByChild("driverId").equalTo(driverId).get().await()

            val history = mutableListOf<PaymentHistoryEntry>()
            snapshot.children.forEach { child ->
                val entry = PaymentHistoryEntry(
                    id = child.key ?: "",
                    driverId = child.child("driverId").getValue(String::class.java) ?: "",
                    bookingId = child.child("bookingId").getValue(String::class.java),
                    amount = child.child("amount").getValue(Double::class.java) ?: 0.0,
                    type = child.child("type").getValue(String::class.java) ?: "",
                    timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                )
                history.add(entry)
            }

            Result.success(history.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Private helper functions

    private suspend fun recordPaymentHistory(
        driverId: String,
        bookingId: String?,
        amount: Double,
        type: String
    ) {
        try {
            val historyRef = database.getReference(PAYMENT_HISTORY_PATH).push()
            val entry = hashMapOf<String, Any>(
                "driverId" to driverId,
                "amount" to amount,
                "type" to type,
                "timestamp" to System.currentTimeMillis()
            )

            bookingId?.let {
                entry["bookingId"] = it
            }

            historyRef.setValue(entry).await()
        } catch (e: Exception) {
            println("Error recording payment history: ${e.message}")
        }
    }
}

data class PaymentHistoryEntry(
    val id: String = "",
    val driverId: String = "",
    val bookingId: String? = null,
    val amount: Double = 0.0,
    val type: String = "", // "TRIP_CONTRIBUTION" or "PAYMENT"
    val timestamp: Long = 0L
)
