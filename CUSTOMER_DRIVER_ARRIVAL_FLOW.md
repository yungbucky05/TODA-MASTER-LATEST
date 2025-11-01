# Customer-Driver Arrival Flow - Real-Time Updates

## Complete Flow Summary

### Driver Side Actions:
1. **Driver accepts booking** → Booking status changes to `ACCEPTED`
2. **Driver clicks "Arrived at Pickup"** → Sets `arrivedAtPickup = true` and `arrivedAtPickupTime = timestamp`
3. **5-minute timer starts automatically** on both driver and customer apps
4. **If customer doesn't show up** → Auto no-show after 5 minutes, booking status changes to `NO_SHOW`
5. **If customer shows up** → Driver clicks "Start Trip", status changes to `IN_PROGRESS`

### Customer Side Display:
1. **Booking accepted** → Shows active booking card with driver info
2. **Driver arrives** → Notification card appears with countdown timer:
   - "Driver has arrived!"
   - "Please come out in Xm Ys"
   - "Booking will auto-cancel if you don't show up"
3. **Timer runs** → Updates every second in real-time
4. **Last minute warning** → Card turns red when less than 60 seconds remain
5. **Time expires** → Shows "Time's up! Booking may be cancelled."
6. **Trip starts** → Notification disappears, shows "Trip in progress" status

## Data Flow Architecture

### Firebase Structure:
```
bookings/
  {bookingId}/
    status: "ACCEPTED" | "IN_PROGRESS" | "NO_SHOW"
    arrivedAtPickup: boolean
    arrivedAtPickupTime: timestamp (milliseconds)
    assignedDriverId: string
    driverRFID: string
    customerId: string
    ...
```

### Real-Time Data Sync:
1. **Driver updates booking** → Firebase Realtime Database
2. **Firebase broadcasts change** → All listening clients
3. **Customer app receives update** → `EnhancedBookingViewModel.activeBookings` Flow
4. **UI recomposes automatically** → Customer sees updates instantly

## Implementation Details

### Driver Side (DriverInterface.kt):
```kotlin
// When driver clicks "Arrived at Pickup"
viewModel.markArrivedAtPickup(booking.id)
  → Updates Firebase: arrivedAtPickup=true, arrivedAtPickupTime=currentTime
  → Starts no-show monitoring job

// Countdown timer in driver app
LaunchedEffect(booking.arrivedAtPickup) {
    if (booking.arrivedAtPickup) {
        while (true) {
            delay(1000)
            currentTime.value = System.currentTimeMillis()
            // Calculate remaining time
            // Show "Report No Show" button after 5 minutes
        }
    }
}
```

### Customer Side (CustomerInterface.kt):
```kotlin
// Active booking card with arrival notification
if (booking.arrivedAtPickup && booking.arrivedAtPickupTime > 0L) {
    // Show arrival notification card
    // Calculate remaining seconds
    val elapsed = currentTime - booking.arrivedAtPickupTime
    val remaining = (5 * 60 * 1000 - elapsed) / 1000
    
    // Display countdown
    "Please come out in ${minutes}m ${seconds}s"
}

// Real-time countdown timer
LaunchedEffect(booking.arrivedAtPickup) {
    if (booking.arrivedAtPickup) {
        while (true) {
            delay(1000)
            currentTime.value = System.currentTimeMillis()
        }
    }
}
```

### Firebase Service (FirebaseRealtimeDatabaseService.kt):
```kotlin
// Mark driver as arrived
suspend fun markArrivedAtPickup(bookingId: String): Boolean {
    val updates = mapOf(
        "bookings/$bookingId/arrivedAtPickup" to true,
        "bookings/$bookingId/arrivedAtPickupTime" to System.currentTimeMillis()
    )
    database.updateChildren(updates).await()
}

// Report no-show
suspend fun reportNoShow(bookingId: String): Boolean {
    val updates = mapOf(
        "bookings/$bookingId/isNoShow" to true,
        "bookings/$bookingId/status" to "NO_SHOW"
    )
    database.updateChildren(updates).await()
}
```

## Fixes Applied

### 1. Firebase Type Conversion Fix
**Problem**: When `assignedDriverId` and `driverRFID` were stored as numbers instead of strings, the parsing failed.

**Solution**: Updated `convertDataSnapshotToFirebaseBooking()` to handle both types:
```kotlin
assignedDriverId = when (val value = data["assignedDriverId"]) {
    is Number -> value.toString()
    is String -> value
    else -> ""
}
```

### 2. Real-Time Data Flow
**Already Working**: 
- `EnhancedBookingViewModel` observes `activeBookings` from Firebase
- Customer interface receives updates automatically via `collectAsState()`
- UI recomposes when booking data changes

### 3. Debug Logging Added
Added comprehensive logging to track data flow:
```kotlin
LaunchedEffect(booking.arrivedAtPickup, booking.arrivedAtPickupTime) {
    println("=== CUSTOMER: ARRIVAL STATUS UPDATE ===")
    println("Driver arrived: ${booking.arrivedAtPickup}")
    println("Arrival time: ${booking.arrivedAtPickupTime}")
    println("Remaining: ${remaining}s")
}
```

## Testing Checklist

### Driver App:
- [ ] Accept a booking
- [ ] Click "Arrived at Pickup" button
- [ ] Verify countdown timer shows (5 minutes)
- [ ] Wait 5+ minutes and verify "Report No Show" button appears
- [ ] Click "Start Trip" before timer expires
- [ ] Verify timer stops when trip starts

### Customer App:
- [ ] Create a booking
- [ ] Wait for driver to accept
- [ ] When driver clicks "Arrived at Pickup", verify:
  - [ ] Notification card appears instantly
  - [ ] Countdown timer shows and updates every second
  - [ ] Card turns blue when > 1 minute remaining
  - [ ] Card turns red when < 1 minute remaining
  - [ ] "Time's up!" message shows when timer expires
- [ ] When driver clicks "Start Trip", verify:
  - [ ] Notification disappears
  - [ ] Status changes to "Trip in progress"

## Expected Behavior

### Scenario 1: Customer Shows Up
1. Driver arrives → Customer sees notification with timer
2. Customer comes out within 5 minutes
3. Driver clicks "Start Trip" → Timer stops, trip begins
4. ✅ Successful trip

### Scenario 2: Customer No-Show
1. Driver arrives → Customer sees notification with timer
2. Customer doesn't show up
3. Timer expires (5 minutes)
4. Auto no-show triggers → Booking cancelled
5. ❌ No-show recorded

### Scenario 3: Manual No-Show
1. Driver arrives → Customer sees notification with timer
2. Timer runs for 5+ minutes
3. Driver manually clicks "Report No Show"
4. Booking immediately cancelled
5. ❌ No-show recorded

## Troubleshooting

### If customer doesn't see arrival notification:
1. Check Firebase Console → Verify `arrivedAtPickup = true`
2. Check Logcat for: `=== CUSTOMER: ARRIVAL STATUS UPDATE ===`
3. Verify booking status is `ACCEPTED` (not `IN_PROGRESS` or other)
4. Check that `arrivedAtPickupTime` has a valid timestamp

### If timer doesn't countdown:
1. Check Logcat for timer updates (should log every second)
2. Verify `LaunchedEffect(booking.arrivedAtPickup)` is triggering
3. Check that `booking.arrivedAtPickupTime > 0`

### If auto no-show doesn't trigger:
1. Check `EnhancedBookingViewModel.startNoShowMonitoring()`
2. Verify job is running in driver app
3. Check Firebase updates after 5 minutes

## Database Fields Reference

### Booking Object Fields:
```kotlin
arrivedAtPickup: Boolean = false          // Driver has arrived
arrivedAtPickupTime: Long = 0L            // Arrival timestamp (ms)
isNoShow: Boolean = false                 // Customer no-show flag
noShowReportedTime: Long = 0L             // No-show timestamp (ms)
status: BookingStatus                      // ACCEPTED, IN_PROGRESS, NO_SHOW
assignedDriverId: String = ""              // Driver's user ID or RFID
driverRFID: String = ""                    // Driver's RFID number
customerId: String = ""                    // Customer's user ID
```

## Success Indicators

When everything is working correctly, you should see:

**Driver Logcat:**
```
✓ Marked booking XYZ as arrived at pickup
📍 Auto-starting no-show monitoring for booking XYZ
=== QUEUE STATUS UPDATE ===
Driver RFID is in queue: false
```

**Customer Logcat:**
```
=== CUSTOMER: ARRIVAL STATUS UPDATE ===
Driver arrived: true
Arrival time: 1730444444000
Remaining: 300s
=== COMPREHENSIVE DEVICE DEBUG ===
Active bookings count: 1
✅ Found active bookings - Cards should display
```

**Firebase Console:**
```
bookings/
  -OcwTm0FHtnap28bmbUT/
    arrivedAtPickup: true
    arrivedAtPickupTime: 1730444444000
    status: "ACCEPTED"
```

