package com.example.toda.ui.navigation

sealed class AdminScreen(val route: String) {
    object Dashboard : AdminScreen("admin_dashboard")
    object DriverManagement : AdminScreen("driver_management")
    object BookingDetails : AdminScreen("booking_details")
    object Contributions : AdminScreen("contributions")
}
