package com.example.toda.ui.driver

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.toda.data.Driver
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRegistrationStatusScreen(
    driverId: String,
    onContinueToInterface: () -> Unit,
    onLogout: () -> Unit
) {
    var driver by remember { mutableStateOf<Driver?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Photo upload states
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    // Edit profile states
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editLicenseNumber by remember { mutableStateOf("") }
    var editTricyclePlateNumber by remember { mutableStateOf("") }
    var isUpdatingProfile by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            isUploadingPhoto = true

            // Upload to Firebase Storage
            val storageRef = FirebaseStorage.getInstance().reference
            val photoRef = storageRef.child("driver_licenses/${driverId}_${System.currentTimeMillis()}.jpg")

            photoRef.putFile(it)
                .addOnSuccessListener { taskSnapshot ->
                    photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Update driver's license photo URL in database with timestamp
                        val updates = hashMapOf<String, Any>(
                            "licensePhotoURL" to downloadUri.toString(),
                            "licensePhotoUploadedAt" to System.currentTimeMillis() // ADD TIMESTAMP
                        )

                        FirebaseDatabase.getInstance()
                            .getReference("drivers/$driverId")
                            .updateChildren(updates)
                            .addOnSuccessListener {
                                isUploadingPhoto = false
                                Toast.makeText(
                                    context,
                                    "License photo uploaded successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { error ->
                                isUploadingPhoto = false
                                Toast.makeText(
                                    context,
                                    "Failed to update photo: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener { error ->
                    isUploadingPhoto = false
                    Toast.makeText(
                        context,
                        "Failed to upload photo: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // Real-time listener for driver data
    LaunchedEffect(driverId) {
        val driverRef = FirebaseDatabase.getInstance().getReference("drivers/$driverId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        // Read TODA number from both possible field names
                        val todaNumber = snapshot.child("todaNumber").value as? String
                            ?: snapshot.child("todaMembershipId").value as? String ?: ""

                        // Also read name from both possible field names
                        val driverName = snapshot.child("driverName").value as? String
                            ?: snapshot.child("name").value as? String ?: ""

                        driver = Driver(
                            id = driverId,
                            name = driverName,
                            phoneNumber = snapshot.child("phoneNumber").value as? String ?: "",
                            address = snapshot.child("address").value as? String ?: "",
                            licenseNumber = snapshot.child("licenseNumber").value as? String ?: "",
                            tricyclePlateNumber = snapshot.child("tricyclePlateNumber").value as? String ?: "",
                            licensePhotoURL = snapshot.child("licensePhotoURL").value as? String ?: "",
                            verificationStatus = snapshot.child("verificationStatus").value as? String ?: "pending",
                            rejectionReason = snapshot.child("rejectionReason").value as? String ?: "",
                            rfidUID = snapshot.child("rfidUID").value as? String ?: "",
                            todaMembershipId = todaNumber
                        )
                        isLoading = false
                    } catch (e: Exception) {
                        error = "Error loading driver data: ${e.message}"
                        isLoading = false
                    }
                } else {
                    error = "Driver data not found"
                    isLoading = false
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                error = "Database error: ${databaseError.message}"
                isLoading = false
            }
        }

        driverRef.addValueEventListener(listener)
    }

    // Check if driver is ready to access interface
    val isReadyForInterface = driver?.let {
        it.verificationStatus == "verified" &&
        it.rfidUID.isNotEmpty() &&
        it.todaMembershipId.isNotEmpty()
    } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registration Status") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onLogout) {
                            Text("Back to Login")
                        }
                    }
                }
                driver != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Welcome Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF667EEA)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Welcome, ${driver!!.name}!",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = driver!!.phoneNumber,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Verification Status Card
                        VerificationStatusCard(driver!!)

                        // Document Upload Card (if not verified or rejected)
                        if (driver!!.verificationStatus != "verified") {
                            DocumentUploadCard(
                                driver = driver!!,
                                selectedImageUri = selectedImageUri,
                                isUploadingPhoto = isUploadingPhoto,
                                onUploadClick = { imagePickerLauncher.launch("image/*") }
                            )
                        }

                        // RFID and TODA Status Card
                        AssignmentStatusCard(driver!!)

                        // Continue Button (only if verified and assigned)
                        if (isReadyForInterface) {
                            Button(
                                onClick = onContinueToInterface,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Continue to Driver Interface",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Edit Profile Button (only if rejected)
                        if (driver!!.verificationStatus == "rejected") {
                            Button(
                                onClick = {
                                    // Populate edit fields with current data
                                    driver?.let { currentDriver ->
                                        editName = currentDriver.name
                                        editAddress = currentDriver.address
                                        editLicenseNumber = currentDriver.licenseNumber
                                        editTricyclePlateNumber = currentDriver.tricyclePlateNumber
                                        showEditDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2)
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Edit Profile",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Edit Profile Information",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Info message
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEFF6FF)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update your information to match your driver's license",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF1e40af)
                                )
                            )
                        }
                    }

                    // Full Name
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        placeholder = { Text("e.g., Juan Dela Cruz") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Phone Number (Read-only display)
                    OutlinedTextField(
                        value = driver?.phoneNumber ?: "",
                        onValueChange = { },
                        label = { Text("Phone Number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        singleLine = true
                    )

                    // Address
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Address") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        placeholder = { Text("Full address within Barangay 177") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    // License Number
                    OutlinedTextField(
                        value = editLicenseNumber,
                        onValueChange = { editLicenseNumber = it.uppercase() },
                        label = { Text("License Number") },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null)
                        },
                        placeholder = { Text("e.g., N21-34-567890") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Tricycle Plate Number
                    OutlinedTextField(
                        value = editTricyclePlateNumber,
                        onValueChange = { editTricyclePlateNumber = it.uppercase() },
                        label = { Text("Tricycle Plate Number") },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        },
                        placeholder = { Text("e.g., AEE-1234") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Warning about TODA and RFID
                    if (driver?.todaMembershipId?.isNotEmpty() == true || driver?.rfidUID?.isNotEmpty() == true) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3CD)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Note: TODA Number and RFID cannot be changed here.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF92400e),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Contact admin for TODA/RFID changes.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF92400e)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Validate input
                        if (editName.isBlank()) {
                            Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editAddress.isBlank()) {
                            Toast.makeText(context, "Please enter your address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editLicenseNumber.isBlank()) {
                            Toast.makeText(context, "Please enter your license number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editTricyclePlateNumber.isBlank()) {
                            Toast.makeText(context, "Please enter your tricycle plate number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isUpdatingProfile = true

                        val updates = hashMapOf<String, Any>(
                            "name" to editName.trim(), // For users table
                            "driverName" to editName.trim(), // For drivers table (admin panel reads this)
                            "address" to editAddress.trim(),
                            "licenseNumber" to editLicenseNumber.trim().uppercase(),
                            "tricyclePlateNumber" to editTricyclePlateNumber.trim().uppercase(),
                            "profileUpdatedAt" to System.currentTimeMillis(), // Timestamp for auto-reset
                            "verificationStatus" to "pending", // Reset status to pending
                            "rejectionReason" to "" // Clear rejection reason
                        )

                        FirebaseDatabase.getInstance()
                            .getReference("drivers/$driverId")
                            .updateChildren(updates)
                            .addOnSuccessListener {
                                isUpdatingProfile = false
                                showEditDialog = false
                                Toast.makeText(
                                    context,
                                    "Profile updated successfully. Your documents will be re-reviewed.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { error ->
                                isUpdatingProfile = false
                                Toast.makeText(
                                    context,
                                    "Failed to update profile: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    enabled = !isUpdatingProfile
                ) {
                    if (isUpdatingProfile) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isUpdatingProfile) "Saving..." else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    enabled = !isUpdatingProfile
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VerificationStatusCard(driver: Driver) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📄 Document Verification",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                val (statusText, statusColor, statusBg) = when (driver.verificationStatus) {
                    "verified" -> Triple("✅ Verified", Color(0xFF065f46), Color(0xFFd1fae5))
                    "rejected" -> Triple("❌ Rejected", Color(0xFF991b1b), Color(0xFFfee2e2))
                    else -> Triple("⏳ Pending", Color(0xFF92400e), Color(0xFFfef3c7))
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider()

            // Status-specific messages
            when (driver.verificationStatus) {
                "verified" -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFECFDF5)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Your documents have been verified and approved!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF065f46)
                                )
                            )
                        }
                    }
                }
                "rejected" -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Document Rejected",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400e)
                                    )
                                )
                            }

                            if (driver.rejectionReason.isNotEmpty()) {
                                Text(
                                    text = "Reason:",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF92400e)
                                    )
                                )
                                Text(
                                    text = driver.rejectionReason,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF92400e)
                                    )
                                )
                            }

                            Text(
                                text = "Please upload a corrected license photo below.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF92400e),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                else -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEFF6FF)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Your application is under review. You'll be notified once the admin verifies your documents.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF1e40af)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentUploadCard(
    driver: Driver,
    selectedImageUri: Uri?,
    isUploadingPhoto: Boolean,
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📸 Driver's License Photo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // License photo preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        driver.licensePhotoURL.isNotEmpty() -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(driver.licensePhotoURL)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Driver's License",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selected License",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No license photo uploaded",
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    if (isUploadingPhoto) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Upload button
            Button(
                onClick = onUploadClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploadingPhoto,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUploadingPhoto) "Uploading..."
                           else if (driver.licensePhotoURL.isNotEmpty()) "Replace License Photo"
                           else "Upload License Photo",
                    fontSize = 16.sp
                )
            }

            if (driver.licensePhotoURL.isEmpty()) {
                Text(
                    text = "⚠️ License photo is required for verification",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD97706)
                    )
                )
            }
        }
    }
}

@Composable
fun AssignmentStatusCard(driver: Driver) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎫 TODA Assignment Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Divider()

            // TODA Number Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = if (driver.todaMembershipId.isNotEmpty()) Color(0xFF059669) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "TODA Number",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (driver.todaMembershipId.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = driver.todaMembershipId,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Not assigned",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            // RFID Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = if (driver.rfidUID.isNotEmpty()) Color(0xFF059669) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "RFID Card",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (driver.rfidUID.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Assigned",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Not assigned",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            // Info message
            if (driver.rfidUID.isEmpty() || driver.todaMembershipId.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEFF6FF)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TODA number and RFID will be assigned by admin after document verification.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF1e40af)
                            )
                        )
                    }
                }
            }
        }
    }
}
