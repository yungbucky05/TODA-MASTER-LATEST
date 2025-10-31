package com.example.toda.utils

import com.example.toda.data.DiscountType
import com.example.toda.data.UserProfile

/**
 * FeeCalculator contains pure functions for computing fees.
 */
object FeeCalculator {
    /**
     * Computes the convenience/system fee based on the user's verified discount status.
     * Rules:
     * - Verified PWD or Senior Citizen: ₱0.00
     * - Verified Student: ₱1.00
     * - Otherwise (no discount, unverified, or unknown): ₱2.00
     */
    fun convenienceFee(userProfile: UserProfile?): Double {
        return when {
            userProfile != null && userProfile.discountType != null && userProfile.discountVerified -> {
                when (userProfile.discountType) {
                    DiscountType.PWD, DiscountType.SENIOR_CITIZEN -> 0.0
                    DiscountType.STUDENT -> 1.0
                }
            }
            else -> 2.0
        }
    }
}

