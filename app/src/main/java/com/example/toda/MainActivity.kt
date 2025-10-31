package com.example.toda

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.toda.data.User
import com.example.toda.data.UserType
import com.example.toda.data.UserProfile
import com.example.toda.data.FirebaseUser
import com.example.toda.data.NotificationState
import com.example.toda.data.BookingStatus
import com.example.toda.data.Booking
import com.example.toda.ui.auth.AuthenticationScreen
import com.example.toda.ui.auth.CustomerLoginScreen
import com.example.toda.ui.common.UserTypeSelection
import com.example.toda.ui.customer.CustomerInterface
import com.example.toda.ui.customer.CustomerDashboardScreen
import com.example.toda.ui.customer.DiscountApplicationScreen
import com.example.toda.ui.driver.DriverLoginScreen
import com.example.toda.ui.driver.DriverInterface
import com.example.toda.ui.driver.DriverRegistrationStatusScreen
import com.example.toda.ui.admin.AdminDashboardScreen
import com.example.toda.ui.admin.AdminDriverManagementScreen
import com.example.toda.ui.admin.AdminBookingDetailsScreen
import com.example.toda.ui.admin.AdminContributionsScreen
import com.example.toda.ui.admin.QueueManagementScreen
import com.example.toda.ui.admin.AdminDiscountApplicationsScreen
import com.example.toda.ui.auth.AdminLoginScreen
import com.example.toda.ui.theme.TODATheme
import com.example.toda.utils.NotificationManager
import com.example.toda.viewmodel.EnhancedBookingViewModel
import com.example.toda.viewmodel.BookingViewModel
import com.example.toda.service.FirebaseSyncService
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.toda.ui.booking.ActiveBookingScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var firebaseSyncService: FirebaseSyncService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Thread.sleep(3000)
        installSplashScreen()
        // Hide the ActionBar (in case it's still shown)
        actionBar?.hide()          // For native ActionBar

        // Force fullscreen (removes status bar)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        notificationManager = NotificationManager(this)

        // Start Firebase sync service to create local JSON files
        firebaseSyncService.startSyncing(this)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Read optional initial screen passed by flavor launcher
        val initialScreen = intent?.getStringExtra("initial_screen")

        setContent {
            TODATheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BookingApp(
                        modifier = Modifier.padding(innerPadding),
                        notificationManager = notificationManager,
                        initialScreen = initialScreen
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the sync service when app is destroyed
        firebaseSyncService.stopSyncing()
    }
}

@Composable
fun BookingApp(
    modifier: Modifier = Modifier,
    notificationManager: NotificationManager,
    initialScreen: String? = null // new optional parameter
) {
    val context = LocalContext.current
    val enhancedBookingViewModel: EnhancedBookingViewModel = hiltViewModel()
    val locationBookingViewModel = remember { BookingViewModel() }

    // Add CustomerLoginViewModel for proper logout functionality
    val customerLoginViewModel: com.example.toda.viewmodel.CustomerLoginViewModel = hiltViewModel()
    // Add Driver and Admin login view models for proper logout
    val driverLoginViewModel: com.example.toda.viewmodel.DriverLoginViewModel = hiltViewModel()
    val adminLoginViewModel: com.example.toda.viewmodel.AdminLoginViewModel = hiltViewModel()

    // Collect real-time data from EnhancedBookingViewModel
    val activeBookings by enhancedBookingViewModel.activeBookings.collectAsState()
    val bookingState by enhancedBookingViewModel.bookingState.collectAsState()

    var selectedUserType by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    // Initialize currentScreen from initialScreen if provided, else default to user_selection
    var currentScreen by remember { mutableStateOf(initialScreen ?: "user_selection") }
    var notificationState by remember {
        mutableStateOf(
            NotificationState(
                hasPermission = notificationManager.hasNotificationPermission()
            )
        )
    }

    // If initialScreen was provided, set selectedUserType accordingly to keep internal state coherent
    LaunchedEffect(initialScreen) {
        when (initialScreen) {
            "customer_login" -> selectedUserType = "customer"
            "driver_login" -> selectedUserType = "driver_login"
            "admin_login" -> selectedUserType = "admin"
        }
    }

    // Track the active booking navigation target and pending snapshot
    var currentActiveBookingId: String? by remember { mutableStateOf<String?>(null) }
    var pendingBookingSnapshot: Booking? by remember { mutableStateOf<Booking?>(null) }

    LaunchedEffect(bookingState.currentBookingId) {
        // Once a booking is created and we receive the server ID, navigate to active booking
        bookingState.currentBookingId?.let { newId ->
            currentActiveBookingId = newId
            currentScreen = "active_booking"
        }
    }

    LaunchedEffect(Unit) {
        locationBookingViewModel.initializeCustomerLocationService(context)
    }

    // Helper function to convert FirebaseUser to User with profile data
    suspend fun convertFirebaseUserToUser(firebaseUser: FirebaseUser): User {
        // Fetch the user's profile from Firebase
        var userProfile: UserProfile? = null
        try {
            enhancedBookingViewModel.getUserProfile(firebaseUser.id).first().let { profile ->
                userProfile = profile
                println("=== PROFILE LOADED IN CONVERSION ===")
                println("User ID: ${firebaseUser.id}")
                println("Profile: $profile")
                println("Discount Type: ${profile?.discountType}")
                println("Discount Verified: ${profile?.discountVerified}")
                println("===================================")
            }
        } catch (e: Exception) {
            println("Error loading user profile: ${e.message}")
        }

        return User(
            id = firebaseUser.id,
            phoneNumber = firebaseUser.phoneNumber,
            name = firebaseUser.name,
            password = "", // Don't store password in User object
            userType = when (firebaseUser.userType) {
                "PASSENGER" -> UserType.PASSENGER
                "DRIVER" -> UserType.DRIVER
                "OPERATOR" -> UserType.OPERATOR
                "TODA_ADMIN" -> UserType.TODA_ADMIN
                else -> UserType.PASSENGER
            },
            isVerified = firebaseUser.isVerified,
            registrationDate = firebaseUser.registrationDate,
            profile = userProfile, // Include the fetched profile with discount data
            todaId = firebaseUser.todaId,
            membershipNumber = firebaseUser.membershipNumber,
            membershipStatus = firebaseUser.membershipStatus
        )
    }


    // Helper function to handle customer logout properly
    fun handleCustomerLogout() {
        // Call the ViewModel's logout function to sign out from Firebase Auth
        customerLoginViewModel.logout()

        // Clear local state
        currentUser = null
        currentUserId = null
        // Keep user type focused on customer so we land on customer login
        selectedUserType = "customer"
        // Redirect to customer login instead of the chooser
        currentScreen = "customer_login"
        locationBookingViewModel.stopCustomerLocationTracking()
    }

    // Helper function to handle driver logout properly
    fun handleDriverLogout() {
        driverLoginViewModel.logout()
        currentUser = null
        currentUserId = null
        selectedUserType = "driver_login"
        currentScreen = "driver_login"
        // Stop any customer location tracking just in case
        locationBookingViewModel.stopCustomerLocationTracking()
    }

    // Helper function to handle admin logout properly
    fun handleAdminLogout() {
        adminLoginViewModel.logout()
        currentUser = null
        currentUserId = null
        selectedUserType = "admin"
        currentScreen = "admin_login"
        locationBookingViewModel.stopCustomerLocationTracking()
    }

    when (currentScreen) {
        "user_selection" -> {
            UserTypeSelection(
                onUserTypeSelected = { userType ->
                    selectedUserType = userType
                    when (userType) {
                        "customer" -> currentScreen = "customer_login"
                        "driver_login" -> currentScreen = "driver_login"
                        "admin" -> currentScreen = "admin_login"
                        else -> currentScreen = "user_selection"
                    }
                }
            )
        }

        "customer_login" -> {
            val coroutineScope = rememberCoroutineScope()
            CustomerLoginScreen(
                onLoginSuccess = { userId, firebaseUser ->
                    currentUserId = userId
                    coroutineScope.launch {
                        // Fetch user profile first before navigation
                        val userWithProfile = convertFirebaseUserToUser(firebaseUser)
                        currentUser = userWithProfile
                        // Only navigate after profile is loaded
                        currentScreen = "customer_dashboard"
                    }
                },
                onRegisterClick = {
                    // Registration is now handled within the CustomerLoginScreen
                    // This callback is kept for backward compatibility but not used
                },
                onBack = {
                    selectedUserType = null
                    currentScreen = "user_selection"
                },
                showBack = (initialScreen == null)
            )
        }

        "driver_login" -> {
            val coroutineScope = rememberCoroutineScope()
            DriverLoginScreen(
                onLoginSuccess = { userId, firebaseUser ->
                    currentUserId = userId
                    coroutineScope.launch {
                        // Fetch user profile first before navigation
                        val userWithProfile = convertFirebaseUserToUser(firebaseUser)
                        currentUser = userWithProfile
                        // Navigate to registration status screen first
                        currentScreen = "driver_registration_status"
                    }
                },
                onRegistrationComplete = {
                    // Navigate back to login mode after successful registration
                    // User can now log in with their new credentials
                    currentScreen = "driver_login"
                },
                onBack = {
                    selectedUserType = null
                    currentScreen = "user_selection"
                },
                showBack = (initialScreen == null)
            )
        }

        "admin_login" -> {
            val coroutineScope = rememberCoroutineScope()
            AdminLoginScreen(
                onLoginSuccess = { userId, firebaseUser ->
                    currentUserId = userId
                    coroutineScope.launch {
                        // Fetch user profile first before navigation
                        val userWithProfile = convertFirebaseUserToUser(firebaseUser)
                        currentUser = userWithProfile
                        // Only navigate after profile is loaded
                        currentScreen = "admin_dashboard"
                    }
                },
                onBack = {
                    selectedUserType = null
                    currentScreen = "user_selection"
                },
                showBack = (initialScreen == null)
            )
        }

        "customer_auth" -> {
            AuthenticationScreen(
                onAuthSuccess = { user ->
                    currentUser = user
                    currentUserId = user.id
                    currentScreen = "customer_dashboard"
                },
                onBack = {
                    currentScreen = "customer_login"
                }
            )
        }

        "customer_dashboard" -> {
            currentUserId?.let { userId ->
                CustomerDashboardScreen(
                    userId = userId,
                    onBookRide = {
                        currentScreen = "customer_interface"
                    },
                    onViewProfile = {
                        // TODO: Navigate to profile screen
                    },
                    onLogout = {
                        handleCustomerLogout()
                    },
                    onApplyDiscount = { category ->
                        // Navigate to discount application screen
                        currentScreen = "discount_application:$category"
                    }
                )
            }
        }

        "customer_interface" -> {
            currentUser?.let { user ->
                // Filter bookings for the current customer - include ALL active statuses for active bookings
                val customerBookings = activeBookings.filter {
                    it.customerId == user.id &&
                    (it.status == BookingStatus.PENDING || it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS)
                }
                val customerBookingHistory = activeBookings.filter {
                    it.customerId == user.id &&
                    (it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED)
                }

                // Debug logging
                LaunchedEffect(activeBookings) {
                    println("CustomerInterface - Total active bookings: ${activeBookings.size}")
                    println("CustomerInterface - Customer bookings: ${customerBookings.size}")
                    customerBookings.forEach { booking ->
                        println("CustomerInterface - Booking ${booking.id}: ${booking.status}")
                    }
                }

                CustomerInterface(
                    user = user,
                    bookings = customerBookings,
                    bookingHistory = customerBookingHistory,
                    customerLocationService = locationBookingViewModel.getCustomerLocationService(),
                    driverTrackingService = locationBookingViewModel.driverTrackingService,
                    onBookingSubmitted = { booking ->
                        // Keep a snapshot so we can show map + waiting state immediately
                        pendingBookingSnapshot = booking
                        // Save to Firebase using EnhancedBookingViewModel
                        enhancedBookingViewModel.createBooking(
                            customerId = booking.customerId,
                            customerName = booking.customerName,
                            phoneNumber = booking.phoneNumber,
                            pickupLocation = booking.pickupLocation,
                            destination = booking.destination,
                            pickupGeoPoint = booking.pickupGeoPoint,
                            dropoffGeoPoint = booking.dropoffGeoPoint,
                            estimatedFare = booking.estimatedFare
                        )
                        // Optimistically navigate while waiting for server to echo the booking
                        currentScreen = "active_booking"
                    },
                    onCompleteBooking = { bookingId ->
                        enhancedBookingViewModel.updateBookingStatusOnly(bookingId, "COMPLETED")
                    },
                    onCancelBooking = { bookingId ->
                        enhancedBookingViewModel.updateBookingStatusOnly(bookingId, "CANCELLED")
                    },
                    onBack = {
                        currentScreen = "customer_dashboard"
                    },
                    onLogout = {
                        handleCustomerLogout()
                    },
                    onOpenActiveBooking = { booking ->
                        // Navigate to Active Booking for the selected booking
                        currentActiveBookingId = booking.id
                        // Keep a local snapshot as a fallback (should already be in server list)
                        pendingBookingSnapshot = booking
                        currentScreen = "active_booking"
                    }
                )
            }
        }

        "active_booking" -> {
            // Show the newly created or existing active booking with map and waiting state
            val user = currentUser
            val bookingId = currentActiveBookingId
            if (user != null) {
                // Prefer the booking from server by ID; fallback to the local snapshot
                val serverBooking = activeBookings.find { it.id == bookingId }
                val bookingForScreen = serverBooking ?: pendingBookingSnapshot?.copy(
                    id = bookingId ?: pendingBookingSnapshot?.id.orEmpty(),
                    status = BookingStatus.PENDING
                )

                if (bookingForScreen != null) {
                    ActiveBookingScreen(
                        booking = bookingForScreen,
                        currentUser = user,
                        onBack = {
                            // Go back to the booking interface
                            currentScreen = "customer_interface"
                        },
                        onNavigateToFullChat = { /* Optional: navigate to a dedicated chat screen */ },
                        onCancelBooking = { bookingIdToCancel ->
                            val idToCancel = bookingState.currentBookingId ?: bookingIdToCancel
                            enhancedBookingViewModel.updateBookingStatusOnly(idToCancel, "CANCELLED")
                            // Clear local active state and return to booking screen
                            currentActiveBookingId = null
                            pendingBookingSnapshot = null
                            currentScreen = "customer_interface"
                        },
                        driver = null,
                        operator = null,
                        onLocationShare = { /* TODO: implement share */ }
                    )
                } else {
                    // If we somehow have neither, fall back to dashboard
                    currentScreen = "customer_dashboard"
                }
            }
        }

        "driver_interface" -> {
            currentUser?.let { user ->
                DriverInterface(
                    user = user,
                    onLogout = {
                        handleDriverLogout()
                    }
                )
            }
        }

        "admin_dashboard" -> {
            AdminDashboardScreen(
                onBack = {
                    selectedUserType = null
                    currentScreen = "user_selection"
                },
                onDriverManagement = {
                    currentScreen = "driver_management"
                },
                onBookingDetails = {
                    currentScreen = "admin_booking_details"
                },
                onContributionTracking = {
                    currentScreen = "admin_contribution_tracking"
                },
                onQueueManagement = {
                    currentScreen = "admin_queue_management"
                },
                onDiscountApplications = {
                    currentScreen = "admin_discount_applications"
                },
                onLogout = {
                    handleAdminLogout()
                }
            )
        }

        "admin_booking_details" -> {
            AdminBookingDetailsScreen(
                onBack = {
                    currentScreen = "admin_dashboard"
                }
            )
        }

        "admin_contribution_tracking" -> {
            AdminContributionsScreen(
                onBack = {
                    currentScreen = "admin_dashboard"
                }
            )
        }

        "driver_management" -> {
            AdminDriverManagementScreen(
                onBack = {
                    currentScreen = "admin_dashboard"
                }
            )
        }

        "admin_queue_management" -> {
            QueueManagementScreen(
                onBack = {
                    currentScreen = "admin_dashboard"
                }
            )
        }

        "admin_discount_applications" -> {
            AdminDiscountApplicationsScreen(
                onBack = {
                    currentScreen = "admin_dashboard"
                }
            )
        }

        "driver_registration_status" -> {
            currentUserId?.let { userId ->
                DriverRegistrationStatusScreen(
                    driverId = userId,
                    onContinueToInterface = {
                        currentScreen = "driver_interface"
                    },
                    onLogout = {
                        handleDriverLogout()
                    }
                )
            }
        }

        else -> {
            // Handle discount_application:CATEGORY screens
            if (currentScreen.startsWith("discount_application:")) {
                val category = currentScreen.removePrefix("discount_application:")
                currentUserId?.let { userId ->
                    DiscountApplicationScreen(
                        userId = userId,
                        discountCategory = category,
                        onNavigateBack = {
                            currentScreen = "customer_dashboard"
                        }
                    )
                }
            }
        }
    }
}