package com.example.toda.utils

import com.example.toda.data.DiscountType
import com.example.toda.data.UserProfile
import com.example.toda.data.UserType
import org.junit.Assert.assertEquals
import org.junit.Test

class FeeCalculatorTest {

    @Test
    fun `convenience fee is 2_00 when userProfile is null`() {
        val fee = FeeCalculator.convenienceFee(null)
        assertEquals(2.0, fee, 0.0)
    }

    @Test
    fun `convenience fee is 2_00 when discountType is null`() {
        val profile = UserProfile(
            phoneNumber = "",
            name = "Test",
            userType = UserType.PASSENGER,
            discountType = null,
            discountVerified = false
        )
        val fee = FeeCalculator.convenienceFee(profile)
        assertEquals(2.0, fee, 0.0)
    }

    @Test
    fun `convenience fee is 1_00 for verified student`() {
        val profile = UserProfile(
            phoneNumber = "",
            name = "Student",
            userType = UserType.PASSENGER,
            discountType = DiscountType.STUDENT,
            discountVerified = true
        )
        val fee = FeeCalculator.convenienceFee(profile)
        assertEquals(1.0, fee, 0.0)
    }

    @Test
    fun `convenience fee is 0_00 for verified PWD`() {
        val profile = UserProfile(
            phoneNumber = "",
            name = "PWD",
            userType = UserType.PASSENGER,
            discountType = DiscountType.PWD,
            discountVerified = true
        )
        val fee = FeeCalculator.convenienceFee(profile)
        assertEquals(0.0, fee, 0.0)
    }

    @Test
    fun `convenience fee is 0_00 for verified Senior Citizen`() {
        val profile = UserProfile(
            phoneNumber = "",
            name = "Senior",
            userType = UserType.PASSENGER,
            discountType = DiscountType.SENIOR_CITIZEN,
            discountVerified = true
        )
        val fee = FeeCalculator.convenienceFee(profile)
        assertEquals(0.0, fee, 0.0)
    }

    @Test
    fun `convenience fee is 2_00 for unverified discount`() {
        val profile = UserProfile(
            phoneNumber = "",
            name = "Unverified Student",
            userType = UserType.PASSENGER,
            discountType = DiscountType.STUDENT,
            discountVerified = false
        )
        val fee = FeeCalculator.convenienceFee(profile)
        assertEquals(2.0, fee, 0.0)
    }
}

