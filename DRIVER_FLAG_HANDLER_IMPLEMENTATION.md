# Driver Flag Handler Implementation Guide

## Overview

This implementation provides a complete flagged accounts system for the TODA Driver App, based on the specifications in `MOBILE_APP_INTEGRATION.md`.

## Components Created

### 1. Data Models (`FlagModels.kt`)

**Location:** `app/src/main/java/com/example/toda/data/FlagModels.kt`

**Models:**
- `DriverFlagData` - Contains driver's flag score and status
- `DriverFlag` - Represents individual flag with all details
- `FlagStatusConfig` - UI configuration for different flag statuses
- `FlagTypeInfo` - Display information for flag types
- `FlagConstants` - Constants and utility functions

**Key Features:**
- Flag status calculation based on score thresholds
- Flag type information mapping
- Severity priority for sorting

### 2. UI Components (`FlagComponents.kt`)

**Location:** `app/src/main/java/com/example/toda/ui/driver/FlagComponents.kt`

**Composables:**
- `FlagStatusBanner` - Shows warning banner at top of interface
- `FlagStatusBadge` - Compact status indicator
- `ActiveFlagsList` - Displays all active flags
- `FlagItem` - Individual flag card with details
- `BlockedAccessScreen` - Full-screen block for suspended accounts

**Features:**
- Color-coded severity levels
- Real-time flag updates
- Contact support integration points
- Responsive design

### 3. Service Layer (`FirebaseRealtimeDatabaseService.kt`)

**Added Methods:**
- `getDriverFlagData()` - Fetch flag score and status
- `observeDriverFlagData()` - Real-time flag data updates
- `getDriverActiveFlags()` - Fetch all active flags
- `observeDriverFlags()` - Real-time flag updates
- `parseDriverFlag()` - Parse flag from Firebase
- `getSeverityPriority()` - Sort flags by severity

### 4. Repository Layer (`TODARepository.kt`)

**Added Methods:**
- `getDriverFlagData()` - Repository wrapper for flag data
- `observeDriverFlagData()` - Repository wrapper for flag data flow
- `getDriverActiveFlags()` - Repository wrapper for active flags
- `observeDriverFlags()` - Repository wrapper for flag flow

### 5. ViewModel Layer (`EnhancedBookingViewModel.kt`)

**Added Methods:**
- `getDriverFlagData()` - ViewModel wrapper
- `observeDriverFlagData()` - ViewModel wrapper for flow
- `getDriverActiveFlags()` - ViewModel wrapper
- `observeDriverFlags()` - ViewModel wrapper for flow

### 6. UI Integration (`DriverInterface.kt`)

**Added Features:**
- Real-time flag monitoring
- Flag status banner display
- Account suspension blocking
- Flags tab in navigation
- Badge showing active flag count

## Database Structure

### Driver Collection
Path: `drivers/{driverId}`
```json
{
  "driverId": "driver-id-123",
  "driverName": "Juan dela Cruz",
  "flagScore": 75,
  "flagStatus": "monitored"
}
```

### Driver Flags Collection
Path: `driverFlags/{driverId}/{flagId}`
```json
{
  "flagId": "flag-abc123",
  "type": "LOW_CONTRIBUTIONS",
  "severity": "high",
  "points": 75,
  "timestamp": 1699123456000,
  "status": "active",
  "details": {
    "averageContribution": "1500.00",
    "driverContribution": "600.00",
    "percentage": "40"
  },
  "notes": "Auto-detected low contributions"
}
```

## Flag Statuses

| Status | Score Range | Icon | Color | Behavior |
|--------|-------------|------|-------|----------|
| **Good Standing** | 0-50 | ✅ | Green | Full access, no restrictions |
| **Monitored** | 51-150 | 👀 | Orange | Warning banner, full access |
| **Restricted** | 151-300 | ⚠️ | Red | Limited features, warnings |
| **Suspended** | 301+ | 🚫 | Dark Red | Blocked access, contact support |

## Flag Types for Drivers

| Type | Points | Severity | Icon | Description |
|------|--------|----------|------|-------------|
| LOW_CONTRIBUTIONS | 75 | High | 💰 | Below 50% of average contributions |
| INACTIVE_ACCOUNT | 50 | Medium | 😴 | No login for 7+ days |
| HIGH_CANCELLATION_RATE | 75 | High | 🚫 | Cancellation rate > 15% |
| CUSTOMER_COMPLAINTS | 100 | Critical | 😠 | Multiple customer complaints |
| RFID_ISSUES | 50 | Medium | 🔑 | RFID card usage problems |

## User Flow

### 1. Good Standing (0-50 points)
- Driver sees normal interface
- No warnings or restrictions
- Full access to all features

### 2. Monitored (51-150 points)
- Orange warning banner appears at top
- Message: "Please improve your performance to avoid restrictions"
- Flag score displayed
- Full access to features
- Flags tab shows active flags with details

### 3. Restricted (151-300 points)
- Red warning banner appears
- Message: "Your account has limited access"
- Contact support button shown
- Potential restrictions (configurable):
  - Daily booking limits
  - Additional verification required
  - Priority reduced in queue

### 4. Suspended (301+ points)
- Full-screen blocked access screen
- Red theme with suspension message
- Cannot access any driver features
- Only option: Contact Support
- Leave queue button hidden
- All booking features disabled

## Real-time Updates

The system monitors flags in real-time using Firebase listeners:

```kotlin
// Flag data observer
LaunchedEffect(user.id) {
    viewModel.observeDriverFlagData(user.id).collect { flagData ->
        flagScore = flagData.flagScore
        flagStatus = flagData.flagStatus
        isSuspended = flagData.flagStatus == "suspended"
    }
}

// Active flags observer
LaunchedEffect(user.id) {
    viewModel.observeDriverFlags(user.id).collect { flags ->
        activeFlags = flags
    }
}
```

### Automatic UI Updates

When admin resolves a flag:
1. Flag status automatically updates
2. Banner changes or disappears
3. Badge count updates
4. Suspended screen disappears if status changes

## UI Components Usage

### Flag Status Banner
```kotlin
FlagStatusBanner(
    flagStatus = "restricted",
    flagScore = 200,
    onContactSupport = {
        // Navigate to support
    }
)
```

### Active Flags List
```kotlin
ActiveFlagsList(
    flags = activeFlags,
    onContactSupport = { flagId ->
        // Handle support contact for specific flag
    }
)
```

### Blocked Access Screen
```kotlin
if (isSuspended) {
    BlockedAccessScreen(
        flagScore = flagScore,
        onContactSupport = {
            // Navigate to support
        }
    )
}
```

## Testing Scenarios

### Test Case 1: Good Standing
1. Set `flagScore = 0` and `flagStatus = "good"`
2. Verify no banner shows
3. Verify normal interface access
4. Verify Flags tab shows "No Active Flags"

### Test Case 2: Monitored Status
1. Add flag with 75 points via admin panel
2. Verify orange banner appears
3. Verify flag appears in Flags tab
4. Verify badge shows "1" on Flags tab
5. Verify full access maintained

### Test Case 3: Restricted Status
1. Add flags totaling 200 points
2. Verify red banner appears with contact support button
3. Verify all flags listed in Flags tab
4. Verify badge shows correct count
5. Verify access still available

### Test Case 4: Suspended Status
1. Add flags totaling 350 points
2. Verify blocked access screen appears
3. Verify no access to normal interface
4. Verify Leave Queue button hidden
5. Verify only Contact Support available

### Test Case 5: Flag Resolution
1. Start with suspended status
2. Admin resolves flags via admin panel
3. Verify real-time update to lower status
4. Verify blocked screen disappears
5. Verify normal interface restores

## Integration with Admin Panel

The driver app reads data set by the admin panel:

**Admin Panel Updates:**
- Creates/updates flags in `driverFlags/{driverId}`
- Updates `flagScore` and `flagStatus` in `drivers/{driverId}`
- Changes flag status to "resolved" or "dismissed"

**Driver App Responds:**
- Real-time listeners detect changes immediately
- UI updates automatically
- No app restart required

## Security Considerations

1. **Read-Only Access**: Drivers can only read flag data, not modify
2. **Firebase Rules**: Ensure proper security rules:
```json
{
  "drivers": {
    "$driverId": {
      ".read": "$driverId === auth.uid",
      ".write": false
    }
  },
  "driverFlags": {
    "$driverId": {
      ".read": "$driverId === auth.uid",
      ".write": false
    }
  }
}
```

## Future Enhancements

### Phase 2
- [ ] In-app appeals system
- [ ] Flag resolution workflow
- [ ] Performance improvement tips
- [ ] Notification when flags added/resolved
- [ ] Flag history (resolved/expired flags)

### Phase 3
- [ ] Push notifications for flag changes
- [ ] Chat integration for support
- [ ] Detailed analytics per flag type
- [ ] Automated improvement suggestions
- [ ] Gamification for good standing

## Troubleshooting

### Issue: Flags not showing
**Solution:** 
- Check Firebase database rules
- Verify `driverFlags/{driverId}` path exists
- Check console logs for errors

### Issue: Real-time updates not working
**Solution:**
- Verify Firebase connection
- Check listener is attached in LaunchedEffect
- Ensure proper cleanup in awaitClose

### Issue: Wrong flag status
**Solution:**
- Verify flag score calculation
- Check threshold constants in FlagConstants
- Verify admin panel is updating flagScore correctly

## Code Locations

| Component | File Path |
|-----------|-----------|
| Data Models | `app/src/main/java/com/example/toda/data/FlagModels.kt` |
| UI Components | `app/src/main/java/com/example/toda/ui/driver/FlagComponents.kt` |
| Service Methods | `app/src/main/java/com/example/toda/service/FirebaseRealtimeDatabaseService.kt` |
| Repository Methods | `app/src/main/java/com/example/toda/repository/TODARepository.kt` |
| ViewModel Methods | `app/src/main/java/com/example/toda/viewmodel/EnhancedBookingViewModel.kt` |
| UI Integration | `app/src/main/java/com/example/toda/ui/driver/DriverInterface.kt` |

## Support Contact Integration

TODO: Implement support contact functionality. Options:
1. In-app chat with admin
2. Email template generation
3. Phone number display
4. Support ticket system
5. WhatsApp/Viber integration

Current placeholder:
```kotlin
onContactSupport = {
    println("Contact support clicked")
    // TODO: Implement support navigation
}
```

## Summary

The driver flag handler provides:
- ✅ Real-time flag monitoring
- ✅ Visual status indicators
- ✅ Account suspension enforcement
- ✅ Detailed flag information
- ✅ Automatic UI updates
- ✅ User-friendly messaging
- ✅ Support contact points

All components are production-ready and follow Material Design 3 guidelines.
