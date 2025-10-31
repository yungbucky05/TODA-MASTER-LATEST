package com.example.toda.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.toda.R
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.viewmodel.CustomerLoginViewModel
import com.example.toda.data.FirebaseUser
import java.text.SimpleDateFormat
import java.util.*
// Added for Firebase Phone Auth
import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import android.util.Patterns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLoginScreen(
    onLoginSuccess: (String, FirebaseUser) -> Unit,
    onRegisterClick: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
    viewModel: CustomerLoginViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegistrationMode by remember { mutableStateOf(false) }
    var currentRegistrationStep by remember { mutableStateOf(1) }
    val maxRegistrationSteps = 3 // Changed from 4 to 3 steps

    // Basic Information (Step 1)
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") } // optional
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Personal Details (Step 2)
    var address by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf<Long?>(null) }
    var gender by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }

    // Preferences & Terms (Step 3) - removed emergency contact variables
    var smsNotifications by remember { mutableStateOf(true) }
    var bookingUpdates by remember { mutableStateOf(true) }
    var promotionalMessages by remember { mutableStateOf(false) }
    var emergencyAlerts by remember { mutableStateOf(true) }
    var agreesToTerms by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Context / Activity
    val context = LocalContext.current
    val activity = context as? Activity

    // OTP / Phone auth UI state
    var showOtpDialog by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    fun isValidEmail(value: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(value).matches()

    // Helper: Convert PH 11-digit local number to E.164
    fun toE164(phLocal: String): String? {
        val clean = phLocal.filter { it.isDigit() }
        if (clean.length == 11 && clean.startsWith("09")) {
            return "+63" + clean.drop(1)
        }
        // If already starts with country code like +63, accept
        return if (clean.startsWith("63") && clean.length == 12) "+$clean" else null
    }

    // Start phone number verification (sends OTP)
    fun startPhoneVerification(onStarted: () -> Unit = {}) {
        val e164 = toE164(phoneNumber)
        if (activity == null || e164 == null) {
            otpError = "Invalid phone number. Use 09XXXXXXXXX"
            return
        }
        isSendingOtp = true
        otpError = null

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(e164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-retrieval or instant verification
                    // Mark as verifying and continue with credential sign-in then proceed to register
                    isSendingOtp = false
                    isVerifyingOtp = true
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Immediately sign out to avoid interfering with email-based registration
                                FirebaseAuth.getInstance().signOut()
                                isVerifyingOtp = false
                                showOtpDialog = false
                                // Proceed with registration
                                val notificationPreferences = mapOf(
                                    "smsNotifications" to smsNotifications,
                                    "bookingUpdates" to bookingUpdates,
                                    "promotionalMessages" to promotionalMessages,
                                    "emergencyAlerts" to emergencyAlerts
                                )
                                val combinedName = listOfNotNull(
                                    firstName.trim(),
                                    middleName.trim().ifBlank { null },
                                    lastName.trim()
                                ).joinToString(" ")
                                viewModel.register(
                                    combinedName,
                                    phoneNumber,
                                    email,
                                    password,
                                    address,
                                    dateOfBirth,
                                    gender,
                                    occupation,
                                    notificationPreferences
                                )
                            } else {
                                isVerifyingOtp = false
                                otpError = task.exception?.localizedMessage ?: "Auto verification failed"
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isSendingOtp = false
                    otpError = e.localizedMessage ?: "Verification failed"
                }

                override fun onCodeSent(
                    verifId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    isSendingOtp = false
                    verificationId = verifId
                    resendToken = token
                    otpCode = ""
                    showOtpDialog = true
                    onStarted()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendCode() {
        val e164 = toE164(phoneNumber)
        val token = resendToken
        if (activity == null || e164 == null || token == null) {
            otpError = "Cannot resend yet. Try again."
            return
        }
        isSendingOtp = true
        otpError = null

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(e164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    isSendingOtp = false
                    isVerifyingOtp = true
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                FirebaseAuth.getInstance().signOut()
                                isVerifyingOtp = false
                                showOtpDialog = false
                                val notificationPreferences = mapOf(
                                    "smsNotifications" to smsNotifications,
                                    "bookingUpdates" to bookingUpdates,
                                    "promotionalMessages" to promotionalMessages,
                                    "emergencyAlerts" to emergencyAlerts
                                )
                                val combinedName = listOfNotNull(
                                    firstName.trim(),
                                    middleName.trim().ifBlank { null },
                                    lastName.trim()
                                ).joinToString(" ")
                                viewModel.register(
                                    combinedName,
                                    phoneNumber,
                                    email,
                                    password,
                                    address,
                                    dateOfBirth,
                                    gender,
                                    occupation,
                                    notificationPreferences
                                )
                            } else {
                                isVerifyingOtp = false
                                otpError = task.exception?.localizedMessage ?: "Auto verification failed"
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isSendingOtp = false
                    otpError = e.localizedMessage ?: "Verification failed"
                }

                override fun onCodeSent(
                    verifId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    isSendingOtp = false
                    verificationId = verifId
                    resendToken = token
                    otpCode = ""
                    showOtpDialog = true
                }
            })
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun confirmOtpAndRegister() {
        val verId = verificationId
        val code = otpCode.filter { it.isDigit() }
        if (verId.isNullOrEmpty() || code.length < 6) {
            otpError = "Enter the 6-digit code"
            return
        }
        isVerifyingOtp = true
        otpError = null
        val credential = PhoneAuthProvider.getCredential(verId, code)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign out to avoid conflicting with email-based account creation
                    FirebaseAuth.getInstance().signOut()
                    isVerifyingOtp = false
                    showOtpDialog = false

                    val notificationPreferences = mapOf(
                        "smsNotifications" to smsNotifications,
                        "bookingUpdates" to bookingUpdates,
                        "promotionalMessages" to promotionalMessages,
                        "emergencyAlerts" to emergencyAlerts
                    )
                    val combinedName = listOfNotNull(
                        firstName.trim(),
                        middleName.trim().ifBlank { null },
                        lastName.trim()
                    ).joinToString(" ")
                    viewModel.register(
                        combinedName,
                        phoneNumber,
                        email,
                        password,
                        address,
                        dateOfBirth,
                        gender,
                        occupation,
                        notificationPreferences
                    )
                } else {
                    isVerifyingOtp = false
                    otpError = task.exception?.localizedMessage ?: "Invalid code. Please try again"
                }
            }
    }

    // Handle successful login
    LaunchedEffect(loginState.isSuccess, currentUser) {
        if (loginState.isSuccess && loginState.userId != null && currentUser != null) {
            onLoginSuccess(loginState.userId!!, currentUser!!)
        }
    }

    // Clear errors when user starts typing
    LaunchedEffect(phoneNumber, password) {
        if (loginState.error != null) {
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
        // App Logo/Title
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
//                containerColor = Color.White // 👈 This makes the card background white
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.moto_logo_nobg),
                    contentDescription = "App Splash Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "TODA Booking",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Passenger Portal - Barangay 177, Caloocan City",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mode Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        isRegistrationMode = false
                        currentRegistrationStep = 1 // Reset to first step
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (!isRegistrationMode) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Sign In")
                }
                Button(
                    onClick = { isRegistrationMode = true },
                    modifier = Modifier.weight(1f),
                    colors = if (isRegistrationMode) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Register")
                }
            }
        }

        // Registration Progress Indicator (only show in registration mode)
        if (isRegistrationMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registration Progress",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step $currentRegistrationStep of $maxRegistrationSteps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { currentRegistrationStep.toFloat() / maxRegistrationSteps },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Login/Registration Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isRegistrationMode) {
                        when (currentRegistrationStep) {
                            1 -> "Basic Information"
                            2 -> "Personal Details"
                            3 -> "Preferences & Terms"
                            else -> "Passenger Registration"
                        }
                    } else "Passenger Login",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Error message
                if (loginState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!isRegistrationMode) "Incorrect number or password" else loginState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Content based on mode and step
                if (isRegistrationMode) {
                    when (currentRegistrationStep) {
                        1 -> {
                            // Basic Information Step
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("First Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading
                            )

                            OutlinedTextField(
                                value = middleName,
                                onValueChange = { middleName = it },
                                label = { Text("Middle Name (optional)") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading
                            )

                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Last Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it.trim() },
                                label = { Text("Email") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading,
                                isError = email.isNotEmpty() && !isValidEmail(email),
                                supportingText = {
                                    if (email.isNotEmpty() && !isValidEmail(email)) {
                                        Text(
                                            text = "Enter a valid email address",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { newValue ->
                                    // Only allow numeric characters
                                    val numericOnly = newValue.filter { it.isDigit() }
                                    // Limit to 11 digits only
                                    if (numericOnly.length <= 11) {
                                        phoneNumber = numericOnly
                                    }
                                },
                                label = { Text("Phone Number") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                placeholder = { Text("09XXXXXXXXX") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading,
                                supportingText = {
                                    Text(
                                        text = "${phoneNumber.length}/11 digits",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (phoneNumber.length == 11)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None
                                                     else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading,
                                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                                supportingText = {
                                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                        Text(
                                            text = "Passwords do not match",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            )
                        }
                        2 -> {
                            // Personal Details Step
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Address") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading,
                                supportingText = {
                                    Text(
                                        text = "Address within Barangay 177, Caloocan City",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            )

                            // Date of Birth Field with working picker
                            var showDatePicker by remember { mutableStateOf(false) }
                            val dateFormatter = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }

                            OutlinedTextField(
                                value = dateOfBirth?.let { dateFormatter.format(Date(it)) } ?: "",
                                onValueChange = { },
                                label = { Text("Date of Birth") },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading,
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
                                    }
                                },
                                placeholder = { Text("Select date of birth") }
                            )

                            // Date Picker Dialog
                            if (showDatePicker) {
                                var selectedYear by remember { mutableStateOf(2000) }
                                var selectedMonth by remember { mutableStateOf(1) }
                                var selectedDay by remember { mutableStateOf(1) }

                                // Initialize with existing date if available
                                LaunchedEffect(dateOfBirth) {
                                    dateOfBirth?.let { timestamp ->
                                        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
                                        selectedYear = calendar.get(Calendar.YEAR)
                                        selectedMonth = calendar.get(Calendar.MONTH) + 1
                                        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
                                    }
                                }

                                AlertDialog(
                                    onDismissRequest = { showDatePicker = false },
                                    title = { Text("Select Date of Birth") },
                                    text = {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Please select your date of birth:",
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Month Dropdown
                                                var monthExpanded by remember { mutableStateOf(false) }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    OutlinedButton(
                                                        onClick = { monthExpanded = true },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = when (selectedMonth) {
                                                                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                                                                5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                                                                9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                                                                else -> "Month"
                                                            }
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = monthExpanded,
                                                        onDismissRequest = { monthExpanded = false }
                                                    ) {
                                                        listOf(
                                                            1 to "January", 2 to "February", 3 to "March", 4 to "April",
                                                            5 to "May", 6 to "June", 7 to "July", 8 to "August",
                                                            9 to "September", 10 to "October", 11 to "November", 12 to "December"
                                                        ).forEach { (monthNum, monthName) ->
                                                            DropdownMenuItem(
                                                                text = { Text(monthName) },
                                                                onClick = {
                                                                    selectedMonth = monthNum
                                                                    monthExpanded = false
                                                                    // Adjust day if needed
                                                                    val daysInMonth = when (monthNum) {
                                                                        1, 3, 5, 7, 8, 10, 12 -> 31
                                                                        4, 6, 9, 11 -> 30
                                                                        2 -> if (selectedYear % 4 == 0) 29 else 28
                                                                        else -> 31
                                                                    }
                                                                    if (selectedDay > daysInMonth) {
                                                                        selectedDay = daysInMonth
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                // Day Dropdown
                                                var dayExpanded by remember { mutableStateOf(false) }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    OutlinedButton(
                                                        onClick = { dayExpanded = true },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(text = selectedDay.toString())
                                                    }
                                                    DropdownMenu(
                                                        expanded = dayExpanded,
                                                        onDismissRequest = { dayExpanded = false }
                                                    ) {
                                                        val daysInMonth = when (selectedMonth) {
                                                            1, 3, 5, 7, 8, 10, 12 -> 31
                                                            4, 6, 9, 11 -> 30
                                                            2 -> if (selectedYear % 4 == 0) 29 else 28
                                                            else -> 31
                                                        }
                                                        (1..daysInMonth).forEach { day ->
                                                            DropdownMenuItem(
                                                                text = { Text(day.toString()) },
                                                                onClick = {
                                                                    selectedDay = day
                                                                    dayExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                // Year Dropdown
                                                var yearExpanded by remember { mutableStateOf(false) }
                                                Box(modifier = Modifier.weight(1f)) {
                                                    OutlinedButton(
                                                        onClick = { yearExpanded = true },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(text = selectedYear.toString())
                                                    }
                                                    DropdownMenu(
                                                        expanded = yearExpanded,
                                                        onDismissRequest = { yearExpanded = false }
                                                    ) {
                                                        (1950..2010).reversed().forEach { year ->
                                                            DropdownMenuItem(
                                                                text = { Text(year.toString()) },
                                                                onClick = {
                                                                    selectedYear = year
                                                                    yearExpanded = false
                                                                    // Adjust February 29th for non-leap years
                                                                    if (selectedMonth == 2 && selectedDay == 29 && year % 4 != 0) {
                                                                        selectedDay = 28
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Selected: ${
                                                    when (selectedMonth) {
                                                        1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
                                                        5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
                                                        9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
                                                        else -> ""
                                                    }
                                                } $selectedDay, $selectedYear",
                                                modifier = Modifier.padding(top = 16.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                val calendar = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, selectedYear)
                                                    set(Calendar.MONTH, selectedMonth - 1)
                                                    set(Calendar.DAY_OF_MONTH, selectedDay)
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                dateOfBirth = calendar.timeInMillis
                                                showDatePicker = false
                                            }
                                        ) {
                                            Text("OK")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDatePicker = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            // Gender Selection
                            var expandedGender by remember { mutableStateOf(false) }
                            val genderOptions = listOf("Male", "Female")

                            ExposedDropdownMenuBox(
                                expanded = expandedGender,
                                onExpandedChange = { expandedGender = !expandedGender }
                            ) {
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Sex") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = !loginState.isLoading
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedGender,
                                    onDismissRequest = { expandedGender = false }
                                ) {
                                    genderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                gender = option
                                                expandedGender = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = occupation,
                                onValueChange = { occupation = it },
                                label = { Text("Occupation") },
                                leadingIcon = {
                                    Icon(Icons.Default.Work, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loginState.isLoading
                            )
                        }
                        3 -> {
                            // Preferences & Terms Step
                            Text(
                                text = "Notification Preferences",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = smsNotifications,
                                    onCheckedChange = { smsNotifications = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SMS Notifications")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = bookingUpdates,
                                    onCheckedChange = { bookingUpdates = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Booking Updates")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = promotionalMessages,
                                    onCheckedChange = { promotionalMessages = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Promotional Messages")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = emergencyAlerts,
                                    onCheckedChange = { emergencyAlerts = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Emergency Alerts")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = agreesToTerms,
                                            onCheckedChange = { agreesToTerms = it }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "I agree to the Terms and Conditions and Privacy Policy",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Navigation buttons for registration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentRegistrationStep > 1) {
                            OutlinedButton(
                                onClick = { currentRegistrationStep-- },
                                enabled = !loginState.isLoading
                            ) {
                                Text("Previous")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp)) // Placeholder
                        }

                        if (currentRegistrationStep < maxRegistrationSteps) {
                            Button(
                                onClick = {
                                    // Validate current step before proceeding
                                    val canProceed = when (currentRegistrationStep) {
                                        1 -> firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && isValidEmail(email) && phoneNumber.isNotBlank() &&
                                             password.isNotBlank() && password == confirmPassword
                                        2 -> address.isNotBlank() && gender.isNotBlank() && occupation.isNotBlank()
                                        else -> true
                                    }
                                    if (canProceed) {
                                        currentRegistrationStep++
                                    }
                                },
                                enabled = !loginState.isLoading
                            ) {
                                Text("Next")
                            }
                        } else {
                            Button(
                                onClick = {
                                    // Handle final registration submission with OTP verification first
                                    // Quick validations
                                    if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || !isValidEmail(email) || phoneNumber.length != 11 || !phoneNumber.startsWith("09") ||
                                        password.isBlank() || password != confirmPassword ||
                                        address.isBlank() || gender.isBlank() || !agreesToTerms
                                    ) {
                                        otpError = "Please complete all required fields correctly"
                                        return@Button
                                    }
                                    startPhoneVerification()
                                },
                                enabled = !loginState.isLoading && agreesToTerms &&
                                          firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && isValidEmail(email) && phoneNumber.isNotBlank() &&
                                          password.isNotBlank() && password == confirmPassword &&
                                          address.isNotBlank() && gender.isNotBlank()
                            ) {
                                if (loginState.isLoading || isSendingOtp || isVerifyingOtp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    when {
                                        isSendingOtp -> "Sending OTP..."
                                        isVerifyingOtp -> "Verifying..."
                                        loginState.isLoading -> "Creating Account..."
                                        else -> "Complete Registration"
                                    }
                                )
                            }
                        }
                    }

                    // OTP Dialog UI
                    if (showOtpDialog) {
                        AlertDialog(
                            onDismissRequest = { if (!isVerifyingOtp) showOtpDialog = false },
                            title = { Text("Verify Phone Number") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("We've sent a 6-digit code to your phone.")
                                    OutlinedTextField(
                                        value = otpCode,
                                        onValueChange = { new ->
                                            val digits = new.filter { it.isDigit() }
                                            if (digits.length <= 6) otpCode = digits
                                        },
                                        label = { Text("Enter OTP") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        enabled = !isVerifyingOtp
                                    )
                                    if (otpError != null) {
                                        Text(
                                            text = otpError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { if (!isSendingOtp && !isVerifyingOtp) resendCode() }, enabled = !isSendingOtp && !isVerifyingOtp) {
                                            Text(if (isSendingOtp) "Resending..." else "Resend Code")
                                        }
                                        TextButton(onClick = { if (!isVerifyingOtp) confirmOtpAndRegister() }, enabled = !isVerifyingOtp) {
                                            if (isVerifyingOtp) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(if (isVerifyingOtp) "Verifying" else "Confirm")
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {}
                        )
                    }
                } else {
                    // Login Mode - Simplified single step
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { newValue ->
                            // Only allow numeric characters
                            val numericOnly = newValue.filter { it.isDigit() }
                            // Limit to 11 digits only
                            if (numericOnly.length <= 11) {
                                phoneNumber = numericOnly
                            }
                        },
                        label = { Text("Phone Number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        placeholder = { Text("09XXXXXXXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loginState.isLoading,
                        supportingText = {
                            Text(
                                text = "${phoneNumber.length}/11 digits",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (phoneNumber.length == 11)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                             else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loginState.isLoading
                    )

                    Button(
                        onClick = {
                            viewModel.login(phoneNumber, password)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loginState.isLoading && phoneNumber.isNotBlank() && password.isNotBlank()
                    ) {
                        if (loginState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (loginState.isLoading) "Signing In..." else "Sign In")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // App Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📍 Service Area",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "We serve within Barangay 177, Caloocan City",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🕒 Operating Hours",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monday to Sunday, 6:00 AM - 10:00 PM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
