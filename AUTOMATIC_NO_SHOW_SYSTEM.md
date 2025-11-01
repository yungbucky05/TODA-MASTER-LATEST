# Automatic No-Show System Implementation

## Overview
This document describes the automatic no-show detection system that monitors customer arrival at pickup points and automatically cancels bookings after a timeout period if the customer doesn't show up.

## System Features

### 1. Driver Side Implementation

#### Arrival Notification
- **"Arrived at Pickup" Button**: Driver clicks this button when reaching the pickup location
- **Timestamp Recording**: System records the exact time of arrival (`arrivedAtPickupTime`)
- **Customer Notification**: Customer is immediately notified when driver arrives
- **Automatic Monitoring**: System starts a 5-minute countdown timer automatically

#### Live Countdown Display
- **Real-time Timer**: Updates every second showing remaining time
- **Visual Feedback**: 
  - Blue/Green background when time > 60 seconds
  - Red background when time < 60 seconds (warning)
- **Format**: Shows "Auto no-show in Xm Ys" format
- **Manual Override**: "Report No Show" button appears after 5 minutes for manual reporting

#### Status Management
- **Before Arrival**: Shows "Arrived at Pickup" button
- **After Arrival**: Shows "Start Trip" button (to begin the journey)
- **Trip Started**: Automatic monitoring stops when driver starts the trip
- **No Show Reported**: Booking status changes to NO_SHOW

### 2. Customer Side Implementation

#### Arrival Alert
- **Prominent Notification Card**: Appears when driver arrives at pickup point
- **Attention-Grabbing Design**: 
  - Large notification icon
  - Bold "Driver has arrived!" message
  - Color-coded background (blue → red as time runs out)

#### Countdown Timer
- **Real-time Display**: Shows exact remaining time
- **Clear Instructions**: "Please come out in Xm Ys"
- **Warning Message**: "Booking will auto-cancel if you don't show up"
- **Time's Up Alert**: "Time's up! Booking may be cancelled." when countdown reaches zero

#### Visual Feedback
- **Color Transitions**:
  - Safe zone (>60s): Light blue background (#E3F2FD)
  - Warning zone (<60s): Light red background (#FFEBEE)
- **Icon Changes**: Notification bell icon changes color with urgency

### 3. Backend Logic

#### EnhancedBookingViewModel Implementation

```kotlin
// No-show timeout constant
private val NO_SHOW_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes

// Start monitoring when driver arrives
fun startNoShowMonitoring(bookingId: String, arrivedAtPickupTime: Long) {
    // Calculate remaining time
    val timeElapsed = System.currentTimeMillis() - arrivedAtPickupTime
    val remainingTime = NO_SHOW_TIMEOUT_MS - timeElapsed
    
    if (remainingTime > 0) {
        // Wait for remaining time
        delay(remainingTime)
        
        // Check if still waiting (not started)
        if (booking.status == ACCEPTED && booking.arrivedAtPickup) {
            reportNoShow(bookingId)  // Auto-report
        }
    }
}

// Stop monitoring when trip starts
fun stopNoShowMonitoring(bookingId: String) {
    // Cancel the monitoring job
    noShowMonitorJobs[bookingId]?.cancel()
}
```

#### Database Updates

When driver marks arrival:
```
bookings/{bookingId}/arrivedAtPickup: true
bookings/{bookingId}/arrivedAtPickupTime: <timestamp>
```

When no-show is reported (auto or manual):
```
bookings/{bookingId}/isNoShow: true
bookings/{bookingId}/noShowReportedTime: <timestamp>
bookings/{bookingId}/status: "NO_SHOW"
```

## User Flow

### Normal Flow (Customer Shows Up)
1. Driver accepts booking → Status: ACCEPTED
2. Driver arrives at pickup → Clicks "Arrived at Pickup"
3. System records arrival time and starts 5-minute countdown
4. Customer receives notification with countdown
5. Customer comes out within 5 minutes
6. Driver clicks "Start Trip" → Status: IN_PROGRESS
7. Automatic monitoring stops
8. Trip proceeds normally

### No-Show Flow (Customer Doesn't Show Up)
1. Driver accepts booking → Status: ACCEPTED
2. Driver arrives at pickup → Clicks "Arrived at Pickup"
3. System records arrival time and starts 5-minute countdown
4. Customer receives notification with countdown
5. 5 minutes pass with no customer
6. System automatically reports NO_SHOW
7. Booking status changes to NO_SHOW
8. Driver can see "Customer no-show" message
9. Driver can leave and accept next booking

### Manual No-Show Flow
1. Driver accepts booking → Status: ACCEPTED
2. Driver arrives at pickup → Clicks "Arrived at Pickup"
3. After 5 minutes, "Report No Show" button appears
4. Driver manually clicks "Report No Show"
5. Booking status changes to NO_SHOW immediately
6. Driver can leave and accept next booking

## Technical Details

### Timer Implementation
- **Technology**: Kotlin Coroutines with `delay()`
- **Update Frequency**: Every 1 second
- **Persistence**: Survives screen rotations using `remember { mutableStateOf() }`
- **Cleanup**: Automatically stopped when trip starts or booking completes

### State Management
- **Reactive**: Uses `LaunchedEffect` for automatic updates
- **Real-time**: Both driver and customer see synchronized countdowns
- **Memory Efficient**: Timers only run when needed

### Error Handling
- **Network Issues**: Arrival time persisted in Firebase
- **App Restart**: Countdown recalculates from stored arrival time
- **Race Conditions**: Checks booking status before auto-reporting

## Benefits

### For Drivers
✅ Fair system - automatic documentation of wait time
✅ No disputes about wait duration
✅ Can report no-show after fair waiting period
✅ Clear visual feedback of remaining time
✅ Option to start trip immediately if customer shows up

### For Customers
✅ Clear notification when driver arrives
✅ Exact countdown showing how much time they have
✅ Fair 5-minute window to get to pickup point
✅ Visual warnings as time runs low
✅ Prevents accidental cancellations

### For System Administrators
✅ Automatic handling reduces manual intervention
✅ Timestamped records for dispute resolution
✅ Standardized 5-minute policy across all bookings
✅ Data analytics on no-show patterns

## Configuration

### Timeout Duration
Current setting: **5 minutes (300 seconds)**

To change the timeout, modify in `EnhancedBookingViewModel.kt`:
```kotlin
private val NO_SHOW_TIMEOUT_MS = 5 * 60 * 1000L  // Change 5 to desired minutes
```

### Visual Thresholds
Warning color threshold: **60 seconds**

To change, modify in both `DriverInterface.kt` and `CustomerInterface.kt`:
```kotlin
if (remainingSeconds > 60)  // Change 60 to desired threshold
```

## Testing Guide

### Test Scenario 1: Normal Completion
1. Create a booking as customer
2. Accept as driver
3. Click "Arrived at Pickup"
4. Verify customer sees notification with countdown
5. Verify driver sees countdown
6. Click "Start Trip" before 5 minutes
7. Verify countdown stops
8. Complete trip normally

### Test Scenario 2: Automatic No-Show
1. Create a booking as customer
2. Accept as driver
3. Click "Arrived at Pickup"
4. Wait for 5 minutes without clicking "Start Trip"
5. Verify booking automatically changes to NO_SHOW
6. Verify both interfaces update correctly

### Test Scenario 3: Manual No-Show
1. Create a booking as customer
2. Accept as driver
3. Click "Arrived at Pickup"
4. Wait for 5+ minutes
5. Click "Report No Show" button
6. Verify booking changes to NO_SHOW
7. Verify customer interface updates

## Database Schema

### Booking Fields
```json
{
  "arrivedAtPickup": false,          // Boolean - driver arrived flag
  "arrivedAtPickupTime": 0,          // Long - timestamp of arrival
  "isNoShow": false,                 // Boolean - no-show flag
  "noShowReportedTime": 0,           // Long - timestamp when reported
  "status": "ACCEPTED"               // String - booking status
}
```

## Future Enhancements

### Potential Improvements
1. **Configurable Timeout**: Allow different timeout periods based on location/time
2. **Push Notifications**: Send push notification to customer when driver arrives
3. **Sound Alert**: Add sound notification on customer device
4. **Grace Period**: Add 30-second grace period before strict enforcement
5. **Analytics Dashboard**: Track no-show rates and patterns
6. **Customer Penalties**: Implement penalty system for repeat offenders
7. **Driver Compensation**: Automatic compensation for no-show incidents

## Troubleshooting

### Issue: Timer not updating
**Solution**: Check if LaunchedEffect is properly triggered. Verify booking.arrivedAtPickup is true.

### Issue: Countdown doesn't stop when trip starts
**Solution**: Verify stopNoShowMonitoring() is called when status changes to IN_PROGRESS.

### Issue: Different times shown on driver vs customer
**Solution**: Both calculate from same arrivedAtPickupTime. Check Firebase sync.

### Issue: Auto no-show triggers too early
**Solution**: Verify NO_SHOW_TIMEOUT_MS constant is set correctly (300000 ms = 5 minutes).

## Summary

This automatic no-show system provides a fair, transparent, and automated way to handle customer no-shows at pickup points. It benefits all parties by:
- Giving customers a clear 5-minute window
- Protecting drivers' time with automatic documentation
- Reducing disputes through timestamped records
- Improving system efficiency with automated handling

The system is fully integrated into both driver and customer interfaces with real-time countdown timers and clear visual feedback.

