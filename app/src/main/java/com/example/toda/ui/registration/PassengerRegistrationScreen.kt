package com.example.toda.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.toda.data.*
import com.example.toda.viewmodel.CustomerRegistrationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerRegistrationScreen(
    onRegistrationComplete: (String) -> Unit, // Pass userId when successful
    onBack: () -> Unit,
    viewModel: CustomerRegistrationViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(1) }
    val maxSteps = 3 // Changed from 4 to 3 steps

    // Collect ViewModel states
    val registrationState by viewModel.registrationState.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Basic Information
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Personal Details
    var address by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf<Long?>(null) }
    var gender by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }

    // Preferences & Terms (no emergency contact variables)
    var smsNotifications by remember { mutableStateOf(true) }
    var bookingUpdates by remember { mutableStateOf(true) }
    var promotionalMessages by remember { mutableStateOf(false) }
    var emergencyAlerts by remember { mutableStateOf(true) }
    var agreesToTerms by remember { mutableStateOf(false) }

    // Handle successful registration
    LaunchedEffect(registrationState.isSuccess) {
        if (registrationState.isSuccess && registrationState.userId != null) {
            onRegistrationComplete(registrationState.userId!!)
        }
    }

    // Auto-clear errors when user starts typing
    LaunchedEffect(name, phoneNumber, password, confirmPassword) {
        if (currentStep == 1) viewModel.clearValidationErrors()
    }

    LaunchedEffect(address, dateOfBirth, gender, occupation) {
        if (currentStep == 2) viewModel.clearValidationErrors()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentStep > 1) currentStep-- else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Customer Registration",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step $currentStep of $maxSteps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicator
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / maxSteps },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error/Success messages
        if (registrationState.error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                        text = registrationState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (registrationState.message != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = registrationState.message!!,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Step content
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (currentStep) {
                1 -> BasicInformationStep(
                    name = name,
                    onNameChange = { name = it },
                    phoneNumber = phoneNumber,
                    onPhoneNumberChange = { phoneNumber = it },
                    password = password,
                    onPasswordChange = { password = it },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    validationErrors = validationErrors.toMap(),
                    isCheckingPhone = registrationState.isCheckingPhone
                )
                2 -> PersonalDetailsStep(
                    address = address,
                    onAddressChange = { address = it },
                    dateOfBirth = dateOfBirth,
                    onDateOfBirthChange = { dateOfBirth = it },
                    gender = gender,
                    onGenderChange = { gender = it },
                    occupation = occupation,
                    onOccupationChange = { occupation = it },
                    validationErrors = validationErrors.toMap()
                )
                3 -> PreferencesAndTermsStep(
                    smsNotifications = smsNotifications,
                    onSmsNotificationsChange = { smsNotifications = it },
                    bookingUpdates = bookingUpdates,
                    onBookingUpdatesChange = { bookingUpdates = it },
                    promotionalMessages = promotionalMessages,
                    onPromotionalMessagesChange = { promotionalMessages = it },
                    emergencyAlerts = emergencyAlerts,
                    onEmergencyAlertsChange = { emergencyAlerts = it },
                    agreesToTerms = agreesToTerms,
                    onAgreesToTermsChange = { agreesToTerms = it }
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    enabled = !registrationState.isRegistering
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        when (currentStep) {
                            1 -> {
                                val isValid = viewModel.validateStep1(name, phoneNumber, password, confirmPassword)
                                if (isValid) {
                                    currentStep++
                                }
                            }
                            2 -> {
                                val isValid = viewModel.validateStep2(address, dateOfBirth, gender, occupation)
                                if (isValid) {
                                    currentStep++
                                }
                            }
                            3 -> {
                                viewModel.registerCustomer(
                                    name = name,
                                    phoneNumber = phoneNumber,
                                    password = password,
                                    address = address,
                                    dateOfBirth = dateOfBirth,
                                    gender = gender,
                                    occupation = occupation,
                                    notificationPreferences = NotificationPreferences(
                                        smsNotifications = smsNotifications,
                                        bookingUpdates = bookingUpdates,
                                        promotionalMessages = promotionalMessages,
                                        emergencyAlerts = emergencyAlerts
                                    ),
                                    agreesToTerms = agreesToTerms
                                )
                            }
                        }
                    }
                },
                enabled = !registrationState.isRegistering
            ) {
                if (registrationState.isRegistering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    when {
                        registrationState.isRegistering -> "Creating Account..."
                        registrationState.isCheckingPhone -> "Checking..."
                        currentStep == maxSteps -> "Complete Registration"
                        else -> "Next"
                    }
                )

                if (currentStep < maxSteps && !registrationState.isRegistering) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun BasicInformationStep(
    name: String,
    onNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    validationErrors: Map<String, String>,
    isCheckingPhone: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Let's start with your basic information",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            isError = validationErrors["name"] != null
        )

        validationErrors["name"]?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                // Only allow numeric characters
                val numericOnly = newValue.filter { it.isDigit() }
                // Limit to 11 digits and must start with 09
                if (numericOnly.length <= 11) {
                    onPhoneNumberChange(numericOnly)
                }
            },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            placeholder = { Text("09XXXXXXXXX") },
            isError = validationErrors["phoneNumber"] != null,
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

        validationErrors["phoneNumber"]?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            isError = validationErrors["password"] != null
        )

        validationErrors["password"]?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            isError = validationErrors["confirmPassword"] != null
        )

        validationErrors["confirmPassword"]?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Phone availability check
        if (isCheckingPhone) {
            Text(
                text = "Checking phone availability...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalDetailsStep(
    address: String,
    onAddressChange: (String) -> Unit,
    dateOfBirth: Long?,
    onDateOfBirthChange: (Long?) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    occupation: String,
    onOccupationChange: (String) -> Unit,
    validationErrors: Map<String, String>
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personal Detailsed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
            minLines = 2,
            isError = validationErrors["address"] != null
        )

        validationErrors["address"]?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Date of Birth Field
        OutlinedTextField(
            value = dateOfBirth?.let { dateFormatter.format(Date(it)) } ?: "",
            onValueChange = { },
            label = { Text("Date of Birth") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
                }
            },
            isError = validationErrors["dateOfBirth"] != null
        )

        validationErrors["dateOfBirth"]?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Simple Date Picker Dialog
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
                                            1 -> "Jan"
                                            2 -> "Feb"
                                            3 -> "Mar"
                                            4 -> "Apr"
                                            5 -> "May"
                                            6 -> "Jun"
                                            7 -> "Jul"
                                            8 -> "Aug"
                                            9 -> "Sep"
                                            10 -> "Oct"
                                            11 -> "Nov"
                                            12 -> "Dec"
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
                                                // Adjust day if it exceeds the days in the new month
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
                                set(Calendar.MONTH, selectedMonth - 1) // Calendar uses 0-based months
                                set(Calendar.DAY_OF_MONTH, selectedDay)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onDateOfBirthChange(calendar.timeInMillis)
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

        // Gender selection
        Text(
            text = "Gender",
            style = MaterialTheme.typography.titleMedium
        )

        validationErrors["gender"]?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Male", "Female", "Other").forEach { option ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = gender == option,
                            onClick = { onGenderChange(option) }
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gender == option,
                        onClick = { onGenderChange(option) }
                    )
                    Text(
                        text = option,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = occupation,
            onValueChange = onOccupationChange,
            label = { Text("Occupation (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) }
        )
    }
}

@Composable
private fun PreferencesAndTermsStep(
    smsNotifications: Boolean,
    onSmsNotificationsChange: (Boolean) -> Unit,
    bookingUpdates: Boolean,
    onBookingUpdatesChange: (Boolean) -> Unit,
    promotionalMessages: Boolean,
    onPromotionalMessagesChange: (Boolean) -> Unit,
    emergencyAlerts: Boolean,
    onEmergencyAlertsChange: (Boolean) -> Unit,
    agreesToTerms: Boolean,
    onAgreesToTermsChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Preferences & Terms",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Notification Preferences
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Notification Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                NotificationPreferenceItem(
                    title = "SMS Notifications",
                    description = "Receive SMS updates about your bookings",
                    checked = smsNotifications,
                    onCheckedChange = onSmsNotificationsChange
                )

                NotificationPreferenceItem(
                    title = "Booking Updates",
                    description = "Get notified about booking status changes",
                    checked = bookingUpdates,
                    onCheckedChange = onBookingUpdatesChange
                )

                NotificationPreferenceItem(
                    title = "Emergency Alerts",
                    description = "Receive emergency and safety alerts",
                    checked = emergencyAlerts,
                    onCheckedChange = onEmergencyAlertsChange
                )

                NotificationPreferenceItem(
                    title = "Promotional Messages",
                    description = "Receive updates about promotions and offers",
                    checked = promotionalMessages,
                    onCheckedChange = onPromotionalMessagesChange
                )
            }
        }

        // Terms and Conditions
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Terms and Conditions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "By registering, you agree to:\n\n" +
                            "• Use the service responsibly and respectfully\n" +
                            "• Provide accurate information\n" +
                            "• Follow TODA Barangay 177 guidelines\n" +
                            "• Pay agreed fares for completed trips\n" +
                            "• Respect drivers and their vehicles",
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreesToTerms,
                        onCheckedChange = onAgreesToTermsChange
                    )
                    Text(
                        text = "I agree to the terms and conditions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPreferenceItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

fun getDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0) 29 else 28
        else -> 0
    }
}
