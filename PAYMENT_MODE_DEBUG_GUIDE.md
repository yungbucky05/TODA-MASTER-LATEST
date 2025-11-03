# Payment Mode Debugging Guide

## Issue
You tested a booking and it was accepted, but the booking doesn't have a `paymentMode` field.

## Enhanced Debugging Added

I've added comprehensive logging to the `updateBookingStatusWithChatRoom` function. When a booking is accepted, you should now see these logs in Logcat:

```
=== FETCHING DRIVER PAYMENT MODE ===
Driver ID: {driverId}
Booking ID: {bookingId}
✓ Driver found in drivers table
Driver Data Retrieved:
  - RFID: {rfidUID}
  - Name: {driverName}
  - TODA: {todaNumber}
  - Payment Mode: {paymentMode}
✓ Setting payment mode to: {paymentMode}
=== FINAL UPDATE MAP ===
  bookings/{bookingId}/paymentMode: {paymentMode}
  bookingIndex/{bookingId}/paymentMode: {paymentMode}
✓ Database updated successfully
```

## How to Debug Your Booking

### Step 1: Check Logcat
1. Open Android Studio
2. Go to Logcat (bottom panel)
3. Filter by "FETCHING DRIVER PAYMENT MODE"
4. Look for the logs when you accept a booking

### Step 2: What to Look For

**If you see:**
```
⚠ Driver profile not found in drivers table for ID: {userId}
```
This means the driver doesn't exist in the `drivers` table. The system will fall back to `pay_every_trip`.

**If you see:**
```
✓ Driver found in drivers table
Driver Data Retrieved:
  - Payment Mode: 
```
(Empty payment mode) - This means the driver exists but doesn't have a `paymentMode` field set yet.

**If you see:**
```
⚠ Error fetching driver profile: {error}
```
There's a Firebase connection or permissions issue.

### Step 3: Check Firebase Database Directly

1. Go to Firebase Console
2. Navigate to Realtime Database
3. Find your booking under `bookings/{bookingId}`
4. Check if these fields exist:
   - `paymentMode`
   - `assignedDriverId`
   - `driverRFID`

5. Also check the driver's profile under `drivers/{driverId}`:
   - Does `paymentMode` field exist?
   - What is its value?

### Step 4: Common Issues & Solutions

#### Issue 1: Driver doesn't have paymentMode set
**Symptoms:**
- Driver Data shows: `Payment Mode: ` (empty)
- Booking gets `pay_every_trip` by default

**Solution:**
1. Go to Firebase Console
2. Find the driver under `drivers/{driverId}`
3. Add field: `paymentMode: "pay_later"` or `"pay_every_trip"`
4. Save
5. Test again

#### Issue 2: Driver ID mismatch
**Symptoms:**
- Log shows: `Driver profile not found in drivers table for ID: {userId}`

**Solution:**
1. Check what `userId` is being used
2. Verify this ID exists in Firebase under `drivers/{userId}`
3. The driver ID should match between:
   - The user's auth ID
   - The `drivers` table key
   - The `assignedDriverId` in the booking

#### Issue 3: Payment mode not syncing to booking
**Symptoms:**
- Driver has `paymentMode` set in Firebase
- Booking still doesn't have it
- No error in logs

**Solution:**
1. Check the "FINAL UPDATE MAP" in logs
2. Verify it includes: `bookings/{bookingId}/paymentMode: {value}`
3. If it's there but not in Firebase:
   - Check Firebase permissions
   - Check Firebase connection
   - Try manually setting it in Firebase Console

### Step 5: Manual Fix (If Needed)

If the automatic system isn't working, you can manually set it:

1. **In Firebase Console:**
   ```
   bookings/{bookingId}/paymentMode: "pay_later"
   ```

2. **For the driver:**
   ```
   drivers/{driverId}/paymentMode: "pay_later"
   ```

### Step 6: Test Again

1. Create a new booking
2. Accept it as a driver
3. Watch the Logcat for the debug messages
4. Check Firebase to see if `paymentMode` is now present
5. Check the driver app UI - does the payment mode indicator show?

## What Should Happen (Normal Flow)

1. **Booking Created**: `paymentMode` = "" (empty)
2. **Driver Accepts**: System fetches driver's `paymentMode` from `drivers/{driverId}`
3. **System Updates**: Sets `bookings/{bookingId}/paymentMode` = driver's payment mode
4. **Driver Sees It**: Payment mode indicator appears in booking card

## Expected Log Output (Success)

```
=== FETCHING DRIVER PAYMENT MODE ===
Driver ID: abc123
Booking ID: xyz789
✓ Driver found in drivers table
Driver Data Retrieved:
  - RFID: 1234567890
  - Name: Juan Dela Cruz
  - TODA: T-001
  - Payment Mode: pay_later
✓ Setting payment mode to: pay_later
✓ Driver assignment: ID=abc123, RFID=1234567890, Name=Juan Dela Cruz, TODA=T-001, PaymentMode=pay_later
=== FINAL UPDATE MAP ===
  bookings/xyz789/status: ACCEPTED
  bookings/xyz789/assignedDriverId: abc123
  bookings/xyz789/driverRFID: 1234567890
  bookings/xyz789/driverName: Juan Dela Cruz
  bookings/xyz789/assignedTricycleId: T-001
  bookings/xyz789/todaNumber: T-001
  bookings/xyz789/paymentMode: pay_later
  bookingIndex/xyz789/driverRFID: 1234567890
  bookingIndex/xyz789/paymentMode: pay_later
  bookingIndex/xyz789/status: ACCEPTED
✓ Database updated successfully
```

## Next Steps

1. Run the app and accept a booking
2. Share the Logcat output with the debug messages
3. Share a screenshot of the booking in Firebase Console
4. We'll identify exactly where the issue is occurring

