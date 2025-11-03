package com.example.toda.ui.driver

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.toda.data.FirebaseUser
import com.example.toda.data.Driver
import com.example.toda.viewmodel.DriverLoginViewModel
import com.example.toda.viewmodel.DriverRegistrationViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import android.util.Patterns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverLoginScreen(
    onLoginSuccess: (String, FirebaseUser) -> Unit,
    onRegistrationComplete: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
    loginViewModel: DriverLoginViewModel = hiltViewModel(),
    registrationViewModel: DriverRegistrationViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegistrationMode by remember { mutableStateOf(false) }

    // Registration fields - Updated to match the Driver class
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") } // optional
    var lastName by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") } // required: Male/Female
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var tricyclePlateNumber by remember { mutableStateOf("") }
    var privacyConsentGiven by remember { mutableStateOf(false) }
    var registrationStep by remember { mutableStateOf(1) } // 1 = Privacy Notice, 2 = Registration Form

    val loginState by loginViewModel.loginState.collectAsStateWithLifecycle()
    val currentUser by loginViewModel.currentUser.collectAsStateWithLifecycle()
    val passwordReset by loginViewModel.passwordReset.collectAsStateWithLifecycle()
    val registrationState by registrationViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Context / Activity for PhoneAuth
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

    // Convert PH local number to E.164 +63 format
    fun toE164(input: String): String? {
        val clean = input.filter { it.isDigit() }
        return when {
            clean.length == 11 && clean.startsWith("09") -> "+63" + clean.drop(1)
            clean.length == 12 && clean.startsWith("63") -> "+$clean"
            else -> null
        }
    }

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
                    isSendingOtp = false
                    isVerifyingOtp = true
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                FirebaseAuth.getInstance().signOut()
                                isVerifyingOtp = false
                                showOtpDialog = false
                                // Proceed with driver registration
                                val combinedName = listOfNotNull(
                                    firstName.trim(),
                                    middleName.trim().ifBlank { null },
                                    lastName.trim()
                                ).joinToString(" ")
                                val driver = Driver(
                                    name = combinedName,
                                    address = address,
                                    licenseNumber = licenseNumber,
                                    tricyclePlateNumber = tricyclePlateNumber,
                                    phoneNumber = phoneNumber,
                                    email = email,
                                    password = password
                                )
                                registrationViewModel.submitRegistration(driver)
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
                                val combinedName = listOfNotNull(
                                    firstName.trim(),
                                    middleName.trim().ifBlank { null },
                                    lastName.trim()
                                ).joinToString(" ")
                                val driver = Driver(
                                    name = combinedName,
                                    address = address,
                                    licenseNumber = licenseNumber,
                                    tricyclePlateNumber = tricyclePlateNumber,
                                    phoneNumber = phoneNumber,
                                    email = email,
                                    password = password
                                )
                                registrationViewModel.submitRegistration(driver)
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
                    FirebaseAuth.getInstance().signOut()
                    isVerifyingOtp = false
                    showOtpDialog = false

                    val combinedName = listOfNotNull(
                        firstName.trim(),
                        middleName.trim().ifBlank { null },
                        lastName.trim()
                    ).joinToString(" ")
                    val driver = Driver(
                        name = combinedName,
                        address = address,
                        licenseNumber = licenseNumber,
                        tricyclePlateNumber = tricyclePlateNumber,
                        phoneNumber = phoneNumber,
                        email = email,
                        password = password
                    )
                    registrationViewModel.submitRegistration(driver)
                } else {
                    isVerifyingOtp = false
                    otpError = task.exception?.localizedMessage ?: "Invalid code. Please try again"
                }
            }
    }

    // Guard to avoid double auto-login
    var autoLoginTriggered by remember { mutableStateOf(false) }

    // After successful registration, auto-login with entered credentials
    LaunchedEffect(registrationState.isRegistrationSuccessful) {
        if (registrationState.isRegistrationSuccessful && !autoLoginTriggered) {
            println("=== REGISTRATION SUCCESSFUL ===")
            println("Auto-logging in with phone: $phoneNumber")
            autoLoginTriggered = true
            // Add a small delay to ensure sign-out completes before attempting login
            kotlinx.coroutines.delay(500)
            // Trigger driver login using the just-entered credentials
            loginViewModel.login(phoneNumber, password)
            // Optionally clear registration flags after initiating login
            // registrationViewModel.clearMessages()
        }
    }

    // Handle successful login
    LaunchedEffect(loginState.isSuccess, currentUser) {
        if (loginState.isSuccess && loginState.userId != null && currentUser != null) {
            println("=== LOGIN SUCCESSFUL ===")
            println("User ID: ${loginState.userId}")
            println("User: ${currentUser?.name}")
            println("Calling onLoginSuccess callback...")
            onLoginSuccess(loginState.userId!!, currentUser!!)
        }
    }

    // Clear errors when user starts typing
    LaunchedEffect(phoneNumber, password) {
        if (loginState.error != null) {
            loginViewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {}
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text(
                text = if (isRegistrationMode) {
                    when (registrationStep) {
                        1 -> "Privacy Notice"
                        else -> "Driver Registration"
                    }
                } else "Driver Login",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicator for registration
        if (registrationState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Mode Toggle (only show when not in registration steps)
        if (!isRegistrationMode || registrationStep == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            isRegistrationMode = false
                            registrationStep = 1
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (!isRegistrationMode) ButtonDefaults.buttonColors()
                               else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Sign In")
                    }
                    Button(
                        onClick = {
                            isRegistrationMode = true
                            registrationStep = 1
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (isRegistrationMode) ButtonDefaults.buttonColors()
                               else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Register")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Progress indicator for registration steps
        if (isRegistrationMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step 1 indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (registrationStep >= 1) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (registrationStep > 1) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    "1",
                                    color = if (registrationStep >= 1) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Privacy Notice",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (registrationStep >= 1) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Connector line
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .weight(1f)
                            .background(
                                if (registrationStep > 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )

                    // Step 2 indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (registrationStep >= 2) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "2",
                                color = if (registrationStep >= 2) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Registration Form",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (registrationStep >= 2) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Content based on registration step or login mode
        when {
            !isRegistrationMode -> {
                // Login mode - show welcome message and login form
                // Welcome Message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DriveEta,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Welcome Back, Driver!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Log in to start accepting bookings and manage your trips.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Driver Login",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // Error messages
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
                                        text = "Incorrect number or password",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Phone Number Field
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                val cleaned = it.filter { char -> char.isDigit() || char == '+' }
                                if (cleaned.length <= 11) phoneNumber = cleaned
                            },
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            placeholder = { Text("09XXXXXXXXX or +639XXXXXXXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loginState.isLoading,
                            isError = phoneNumber.isNotEmpty() && validatePhoneNumber(phoneNumber) != null,
                            supportingText = {
                                val phoneError = if (phoneNumber.isNotEmpty()) validatePhoneNumber(phoneNumber) else null
                                if (phoneError != null) {
                                    Text(
                                        text = phoneError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "Enter your registered phone number",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true
                        )

                        // Password Field
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loginState.isLoading,
                            singleLine = true
                        )

                        // Login Button
                        Button(
                            onClick = {
                                loginViewModel.login(phoneNumber, password)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loginState.isLoading &&
                                    phoneNumber.isNotBlank() &&
                                    password.isNotBlank()
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

                        // Forgot password dialog
                        var showResetDialog by remember { mutableStateOf(false) }
                        var resetEmail by remember { mutableStateOf("") }

                        TextButton(onClick = { showResetDialog = true }) {
                            Text("Forgot password?")
                        }

                        if (showResetDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    if (!passwordReset.isSending) {
                                        showResetDialog = false
                                        loginViewModel.clearPasswordReset()
                                        resetEmail = ""
                                    }
                                },
                                title = { Text("Reset password") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Enter the email you used during driver registration.")
                                        OutlinedTextField(
                                            value = resetEmail,
                                            onValueChange = { resetEmail = it.trim() },
                                            label = { Text("Email") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                            singleLine = true,
                                            enabled = !passwordReset.isSending,
                                            isError = resetEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches(),
                                            supportingText = {
                                                val msg = when {
                                                    passwordReset.error != null -> passwordReset.error
                                                    passwordReset.sent -> "If an account exists for this email, a reset link has been sent. Check your inbox and spam folder."
                                                    else -> null
                                                }
                                                if (msg != null) {
                                                    Text(msg, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = { loginViewModel.resetPassword(resetEmail) },
                                        enabled = !passwordReset.isSending && Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()
                                    ) {
                                        if (passwordReset.isSending) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(
                                            when {
                                                passwordReset.isSending -> "Sending..."
                                                passwordReset.sent -> "Resend"
                                                else -> "Send link"
                                            }
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showResetDialog = false
                                            loginViewModel.clearPasswordReset()
                                            resetEmail = ""
                                        }
                                    ) {
                                        Text("Close")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            registrationStep == 1 -> {
                // Step 1: Privacy Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Data Privacy & Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Before we proceed with your driver registration, please read and understand our privacy policy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Notice Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = "Privacy Notice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "We value your privacy. In compliance with the Data Privacy Act of 2012 (RA 10173), we collect your personal data, including your Driver's License Number, solely for the purpose of verifying your identity and eligibility as a registered driver in our system.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Your information will be:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Used only for driver registration, verification, and compliance purposes.\n" +
                                  "• Accessed only by authorized personnel.\n" +
                                  "• Stored securely and protected against unauthorized access.\n" +
                                  "• Retained only for as long as necessary for the stated purpose.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "We will not share, disclose, or sell your information to third parties without your consent, unless required by law.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Consent Statement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = privacyConsentGiven,
                                onCheckedChange = { privacyConsentGiven = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I have read and understood the Privacy Notice. I voluntarily give my consent for TODA Barangay 177 to collect, process, and store my personal information, including my Driver's License Number, for the purpose of driver registration and verification.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!privacyConsentGiven) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You must agree to the Privacy Notice to continue with registration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Continue Button
                        Button(
                            onClick = {
                                registrationStep = 2
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = privacyConsentGiven
                        ) {
                            Text("Continue to Registration")
                        }
                    }
                }
            }

            registrationStep == 2 -> {
                // Step 2: Registration Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DriveEta,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Join TODA Drivers!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Register to start accepting bookings as a TODA driver in Barangay 177.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Registration Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Driver Registration Form",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { registrationStep = 1 }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }
                        }

                        // Error messages
                        if (registrationState.errorMessage != null) {
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
                                        text = registrationState.errorMessage!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // First Name
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            singleLine = true
                        )

                        // Middle Name (optional)
                        OutlinedTextField(
                            value = middleName,
                            onValueChange = { middleName = it },
                            label = { Text("Middle Name (optional)") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            singleLine = true
                        )

                        // Last Name
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            singleLine = true
                        )

                        // Email (required for password recovery)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            isError = email.isNotEmpty() && validateEmail(email) != null,
                            supportingText = {
                                val emailError = if (email.isNotEmpty()) validateEmail(email) else null
                                if (emailError != null) {
                                    Text(
                                        text = emailError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "We'll use this for password resets and account notices",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true
                        )

                        // Sex selection (Male/Female)
                        var sexExpanded by remember { mutableStateOf(false) }
                        val sexOptions = listOf("Male", "Female")
                        ExposedDropdownMenuBox(
                            expanded = sexExpanded,
                            onExpandedChange = { sexExpanded = !sexExpanded }
                        ) {
                            OutlinedTextField(
                                value = sex,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Sex") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                enabled = !registrationState.isLoading,
                                isError = sex.isBlank()
                            )
                            ExposedDropdownMenu(
                                expanded = sexExpanded,
                                onDismissRequest = { sexExpanded = false }
                            ) {
                                sexOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            sex = option
                                            sexExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            supportingText = { Text("Address within Barangay 177, Caloocan City") }
                        )

                        // Driver's License Number
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it.uppercase() },
                            label = { Text("Driver's License Number") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            isError = licenseNumber.isNotEmpty() && validateDriverLicense(licenseNumber) != null,
                            supportingText = {
                                val licenseError = if (licenseNumber.isNotEmpty()) validateDriverLicense(licenseNumber) else null
                                if (licenseError != null) {
                                    Text(
                                        text = licenseError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text("Valid Philippine driver's license required")
                                }
                            },
                            singleLine = true
                        )

                        // Tricycle Plate Number
                        OutlinedTextField(
                            value = tricyclePlateNumber,
                            onValueChange = { tricyclePlateNumber = it.uppercase() },
                            label = { Text("Tricycle Plate Number") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            isError = tricyclePlateNumber.isNotEmpty() && validateTricyclePlate(tricyclePlateNumber) != null,
                            supportingText = {
                                val plateError = if (tricyclePlateNumber.isNotEmpty()) validateTricyclePlate(tricyclePlateNumber) else null
                                if (plateError != null) {
                                    Text(
                                        text = plateError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text("Registered tricycle plate number")
                                }
                            },
                            singleLine = true
                        )

                        // Phone Number Field
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                val cleaned = it.filter { char -> char.isDigit() || char == '+' }
                                if (cleaned.length <= 11) phoneNumber = cleaned
                            },
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            placeholder = { Text("09XXXXXXXXX or +639XXXXXXXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            isError = phoneNumber.isNotEmpty() && validatePhoneNumber(phoneNumber) != null,
                            supportingText = {
                                val phoneError = if (phoneNumber.isNotEmpty()) validatePhoneNumber(phoneNumber) else null
                                if (phoneError != null) {
                                    Text(
                                        text = phoneError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "This will be your login phone number",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true
                        )

                        // Password Field
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            singleLine = true
                        )

                        // Confirm Password Field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !registrationState.isLoading,
                            singleLine = true,
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

                        // Submit Button
                        Button(
                            onClick = {
                                // Validate registration fields before submitting
                                val emailValidation = validateEmail(email)
                                val phoneValidation = validatePhoneNumber(phoneNumber)
                                val licenseValidation = validateDriverLicense(licenseNumber)
                                val plateValidation = validateTricyclePlate(tricyclePlateNumber)

                                if (emailValidation != null) {
                                    registrationViewModel.setValidationError(emailValidation)
                                    return@Button
                                }

                                if (phoneValidation != null) {
                                    registrationViewModel.setValidationError(phoneValidation)
                                    return@Button
                                }

                                if (licenseValidation != null) {
                                    registrationViewModel.setValidationError(licenseValidation)
                                    return@Button
                                }

                                if (plateValidation != null) {
                                    registrationViewModel.setValidationError(plateValidation)
                                    return@Button
                                }

                                // Trigger OTP verification first
                                startPhoneVerification()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isFormValid(firstName, lastName, sex, address, licenseNumber, tricyclePlateNumber, phoneNumber, email, password, confirmPassword, privacyConsentGiven) && !registrationState.isLoading
                        ) {
                            if (registrationState.isLoading || isSendingOtp || isVerifyingOtp) {
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
                                    registrationState.isLoading -> "Creating Account..."
                                    else -> "Submit Application"
                                }
                            )
                        }

                        // OTP Dialog for registration
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

                        // Registration note
                        Text(
                            text = "Note: Your application will be reviewed by TODA administrators. " +
                                    "You will be notified of the approval status via SMS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom information cards
        when {
            !isRegistrationMode -> {
                // Service Information for login mode
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
                            text = "🚗 Driver Benefits",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Organized booking system\n• Fair passenger distribution\n• Community support\n• Earnings tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            registrationStep == 2 -> {
                // Registration requirements
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📋 Driver Registration Requirements",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Valid Philippine driver's license\n" +
                                  "• Registered tricycle with valid documents\n" +
                                  "• Must operate within Barangay 177\n" +
                                  "• TODA Membership ID will be assigned by admin\n" +
                                  "• Application subject to TODA approval",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun isFormValid(
    firstName: String,
    lastName: String,
    sex: String,
    address: String,
    license: String,
    plateNumber: String,
    phone: String,
    email: String,
    password: String,
    confirmPassword: String,
    privacyConsent: Boolean = false
): Boolean {
    return firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            sex.isNotBlank() &&
            address.isNotBlank() &&
            license.isNotBlank() &&
            plateNumber.isNotBlank() &&
            phone.isNotBlank() &&
            email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            password.isNotBlank() &&
            password.length >= 6 &&
            password == confirmPassword &&
            privacyConsent
}

// Validation functions
private fun validatePhoneNumber(phoneNumber: String): String? {
    val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
    return when {
        phoneNumber.isBlank() -> "Phone number is required"
        !cleanNumber.startsWith("09") -> "Please enter a valid Philippine phone number starting with 09"
        cleanNumber.length != 11 -> "Phone number must be exactly 11 digits (e.g., 09XXXXXXXXX)"
        else -> null
    }
}

private fun validateDriverLicense(license: String): String? {
    val regex = Regex("^[A-Z]{1,2}[0-9]{2}-[0-9]{2}-[0-9]{6}$")
    return when {
        license.isBlank() -> "Driver's license number is required"
        !regex.matches(license.trim()) -> "Please enter a valid Philippine Driver's License number (e.g., N12-34-567890)"
        else -> null
    }
}

private fun validateTricyclePlate(plate: String): String? {
    val regex = Regex("^[A-Z]{3}-[0-9]{4}$")
    return when {
        plate.isBlank() -> "Tricycle plate number is required"
        !regex.matches(plate.trim()) -> "Please enter a valid Tricycle Plate Number (e.g., ABC-1234)"
        else -> null
    }
}

private fun validateEmail(email: String): String? {
    return when {
        email.isBlank() -> "Email is required"
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address"
        else -> null
    }
}
