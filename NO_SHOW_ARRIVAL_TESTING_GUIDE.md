# No Show & Arrival at Pickup - Testing Guide

## Overview
This guide will help you test the newly implemented "Arrived at Pickup" and "No Show" features in the TODA Driver App.

## Features Implemented

### 1. **Arrived at Pickup Button**
- Appears when booking status is `ACCEPTED` and driver hasn't arrived yet
- Marks the driver as arrived and records timestamp
- Displays a blue info banner showing waiting time (updates every minute)

### 2. **No Show Button**
- Automatically appears **only after 5 minutes** of waiting at pickup point
- Only visible when booking is in `ACCEPTED` status and driver has marked arrival
- Reports customer as no-show and changes booking status to `NO_SHOW`

---

## Test Prerequisites

### Required Accounts:
1. **Passenger Account** (to create bookings)
   - Phone number and password
   
2. **Driver Account** (to test the features)
   - Must be registered and active
   - Must have RFID assigned
   - Must be in the queue (online)

### Database State Requirements:
- Driver must be in the `queue` table with status = "waiting"
- Driver's RFID must be properly assigned in `drivers` table

---

## Testing Scenarios

### **Scenario 1: Normal Flow - Customer Shows Up**

#### Step 1: Create a Booking (Passenger App)
1. Open the **Passenger App**
2. Login with passenger credentials
3. Create a new booking:
   - Enter pickup location
   - Enter destination
   - Confirm booking

#### Step 2: Accept Booking (Driver App)
1. The booking should **automatically** be assigned to the first driver in queue
2. Open the **Driver App**
3. Login with driver credentials
4. Navigate to the "Bookings" tab
5. You should see the booking in "Active Trips" with status `ACCEPTED`

#### Step 3: Mark Arrival at Pickup
1. Click the **"Arrived at Pickup"** button (blue button with location icon)
2. You should see:
   - Toast message: "Marked as arrived at pickup point"
   - Blue info banner appears showing: "Arrived at pickup • Waiting 0m"
   - The "Arrived at Pickup" button is replaced with **"Start Trip"** button

#### Step 4: Wait and Observe Timer
1. Wait 1 minute
2. The info banner should update to show: "Arrived at pickup • Waiting 1m"
3. Wait another minute
4. Banner updates to: "Arrived at pickup • Waiting 2m"
5. **Note:** The "No Show" button will NOT appear yet (need to wait 5 minutes total)

#### Step 5: Start the Trip (Before 5 Minutes)
1. Click the **"Start Trip"** button
2. Booking status changes to `IN_PROGRESS`
3. Timer and arrival banner disappear
4. "Complete Trip" button appears

#### Step 6: Complete the Trip
1. Click **"Complete Trip"** button
2. Booking status changes to `COMPLETED`
3. Booking moves to "History" tab

---

### **Scenario 2: No Show Flow - Customer Doesn't Show Up**

#### Step 1-3: Same as Scenario 1
Follow Steps 1-3 from Scenario 1 (create booking, accept, mark arrival)

#### Step 4: Wait for 5 Minutes
1. After marking arrival, wait for **5 minutes**
2. The timer will update every minute:
   - "Waiting 1m"
   - "Waiting 2m"
   - "Waiting 3m"
   - "Waiting 4m"
   - "Waiting 5m"

#### Step 5: No Show Button Appears
1. After exactly 5 minutes, a **red "Report No Show" button** appears below the main action buttons
2. This button spans the full width of the card
3. It has a cancel icon and red background

#### Step 6: Report No Show
1. Click the **"Report No Show"** button
2. You should see:
   - Toast message: "Customer no-show reported"
   - Booking disappears from "Active Trips"

#### Step 7: Verify Database Updates
Check Firebase Realtime Database to confirm:
```
bookings/{bookingId}/
  - status: "NO_SHOW"
  - isNoShow: true
  - noShowReportedTime: {timestamp}
  - arrivedAtPickup: true
  - arrivedAtPickupTime: {timestamp}
```

---

## Quick Testing (For Development)

### **Option 1: Modify Timer Duration (For Fast Testing)**

If you want to test without waiting 5 minutes, you can temporarily modify the code:

**File:** `app/src/main/java/com/example/toda/ui/driver/DriverInterface.kt`

**Find this line (around line 865):**
```kotlin
(currentTime.value - booking.arrivedAtPickupTime) >= 5 * 60 * 1000 // 5 minutes
```

**Change to:**
```kotlin
(currentTime.value - booking.arrivedAtPickupTime) >= 10 * 1000 // 10 seconds for testing
```

**Remember to change it back to 5 minutes before production!**

### **Option 2: Manually Update Database**

You can manually set the arrival time in Firebase to simulate a 5-minute wait:

1. Go to Firebase Console → Realtime Database
2. Find your booking: `bookings/{bookingId}`
3. Set these values:
   ```
   arrivedAtPickup: true
   arrivedAtPickupTime: {current_timestamp - 300000}  // 5 minutes ago
   ```
4. Refresh the driver app
5. The "No Show" button should appear immediately

---

## Testing Checklist

### ✅ Arrived at Pickup Feature
- [ ] Button appears when booking status is ACCEPTED
- [ ] Button has location icon and says "Arrived at Pickup"
- [ ] Clicking button shows success toast
- [ ] Blue info banner appears after clicking
- [ ] Timer shows "Waiting 0m" initially
- [ ] Timer updates every minute
- [ ] Button is replaced with "Start Trip" after arrival
- [ ] Database fields are updated correctly:
  - `arrivedAtPickup: true`
  - `arrivedAtPickupTime: {timestamp}`

### ✅ No Show Feature
- [ ] Button does NOT appear before 5 minutes
- [ ] Button appears exactly after 5 minutes of waiting
- [ ] Button is red with cancel icon
- [ ] Button says "Report No Show"
- [ ] Clicking button shows success toast
- [ ] Booking status changes to NO_SHOW
- [ ] Booking disappears from active trips
- [ ] Database fields are updated correctly:
  - `isNoShow: true`
  - `noShowReportedTime: {timestamp}`
  - `status: "NO_SHOW"`

### ✅ Edge Cases
- [ ] No Show button doesn't appear if trip is started before 5 minutes
- [ ] No Show button disappears when status changes to IN_PROGRESS
- [ ] Timer stops updating when trip is started
- [ ] Multiple bookings can be handled independently
- [ ] App doesn't crash if booking is deleted while viewing

---

## Troubleshooting

### Issue: "Arrived at Pickup" button doesn't appear
**Solution:** Check that:
- Booking status is exactly "ACCEPTED"
- The booking's `arrivedAtPickup` field is `false` or doesn't exist
- Driver is properly authenticated

### Issue: "No Show" button never appears
**Solution:** Check that:
1. You clicked "Arrived at Pickup" first
2. Full 5 minutes have passed
3. Booking is still in "ACCEPTED" status (not IN_PROGRESS)
4. Check console logs for timer updates

### Issue: Timer doesn't update
**Solution:**
- The timer only updates every 60 seconds (1 minute)
- Check that the LaunchedEffect is running (see console logs)
- Ensure the booking is still visible on screen

### Issue: Database not updating
**Solution:**
- Check Firebase connection
- Verify driver has proper permissions
- Check console for error messages
- Verify `markArrivedAtPickup` and `reportNoShow` methods are being called

---

## Expected Console Output

### When Marking Arrival:
```
✓ Marked booking {bookingId} as arrived at pickup
```

### When Reporting No Show:
```
✓ Reported no-show for booking {bookingId}
```

### Timer Updates (Every Minute):
```
Waiting time updated: 1 minutes
Waiting time updated: 2 minutes
...
```

---

## Database Structure

### Before Arrival:
```json
{
  "bookings": {
    "{bookingId}": {
      "status": "ACCEPTED",
      "arrivedAtPickup": false,
      "arrivedAtPickupTime": 0,
      "isNoShow": false,
      "noShowReportedTime": 0
    }
  }
}
```

### After Arrival:
```json
{
  "bookings": {
    "{bookingId}": {
      "status": "ACCEPTED",
      "arrivedAtPickup": true,
      "arrivedAtPickupTime": 1730383200000,
      "isNoShow": false,
      "noShowReportedTime": 0
    }
  }
}
```

### After No Show Report:
```json
{
  "bookings": {
    "{bookingId}": {
      "status": "NO_SHOW",
      "arrivedAtPickup": true,
      "arrivedAtPickupTime": 1730383200000,
      "isNoShow": true,
      "noShowReportedTime": 1730383500000
    }
  }
}
```

---

## Visual Reference

### UI States:

**State 1: Before Arrival**
```
┌─────────────────────────────────────┐
│ John Doe               [ACCEPTED]   │
│                                     │
│ Time: 2:30 PM                       │
│ Pick up: Location A                 │
│ Drop off: Location B                │
│ Fare: ₱25.00                        │
│                                     │
│ [Arrived at Pickup] 📍  [Chat] 💬  │
└─────────────────────────────────────┘
```

**State 2: After Arrival (0-4 minutes)**
```
┌─────────────────────────────────────┐
│ John Doe               [ACCEPTED]   │
│                                     │
│ Time: 2:30 PM                       │
│ Pick up: Location A                 │
│ Drop off: Location B                │
│ Fare: ₱25.00                        │
│                                     │
│ ✓ Arrived at pickup • Waiting 2m   │
│                                     │
│ [Start Trip] 🚗        [Chat] 💬    │
└─────────────────────────────────────┘
```

**State 3: After 5+ Minutes (No Show Available)**
```
┌─────────────────────────────────────┐
│ John Doe               [ACCEPTED]   │
│                                     │
│ Time: 2:30 PM                       │
│ Pick up: Location A                 │
│ Drop off: Location B                │
│ Fare: ₱25.00                        │
│                                     │
│ ✓ Arrived at pickup • Waiting 5m   │
│                                     │
│ [Start Trip] 🚗        [Chat] 💬    │
│                                     │
│ [Report No Show] ❌                 │
└─────────────────────────────────────┘
```

---

## Production Deployment Notes

Before deploying to production:

1. ✅ Ensure timer is set to 5 minutes (not reduced for testing)
2. ✅ Test with real device, not just emulator
3. ✅ Verify Firebase security rules allow these updates
4. ✅ Test with slow/unstable internet connection
5. ✅ Test with multiple simultaneous bookings
6. ✅ Document the feature for drivers in user manual
7. ✅ Train drivers on when to use "No Show" feature

---

## Support & Contact

If you encounter any issues during testing:
1. Check Firebase Console for error logs
2. Review Android Studio Logcat for detailed error messages
3. Verify all Firebase methods are properly implemented
4. Ensure all database indexes are created

---

**Last Updated:** October 31, 2025  
**Feature Version:** 1.0  
**Tested On:** Android 8.0+

