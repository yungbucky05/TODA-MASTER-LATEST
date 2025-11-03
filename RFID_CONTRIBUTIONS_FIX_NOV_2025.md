# RFID History and Contributions Not Showing Fix

**Date:** November 3, 2025  
**Issue:** RFID history and contributions aren't showing in driver app

## Problem Diagnosed

The system uses two different RFID field names in the Firebase database:
1. **`rfidNumber`** - Newer field
2. **`rfidUID`** - Legacy field

The `getDriverContributions()` function was only checking `rfidUID`, which meant:
- If a driver's RFID was stored in `rfidNumber`, contributions wouldn't load
- The RFID history tab would also fail to show data
- Drivers would see "No contributions available" even when they had contributions recorded

## Root Cause

In `FirebaseRealtimeDatabaseService.kt`, line ~860, the code was:

```kotlin
val driverRFID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""
```

This only checked the `rfidUID` field, ignoring `rfidNumber`.

## Solution Applied

Updated `getDriverContributions()` to check both fields:

```kotlin
// Check both rfidNumber (new) and rfidUID (legacy)
val rfidNumber = driverSnapshot.child("rfidNumber").getValue(String::class.java) ?: ""
val rfidUID = driverSnapshot.child("rfidUID").getValue(String::class.java) ?: ""

// Prioritize rfidNumber if it exists, otherwise use rfidUID
val driverRFID = if (rfidNumber.isNotEmpty()) rfidNumber else rfidUID
```

Also updated `recordCoinInsertion()` function to search for drivers using both RFID fields:

```kotlin
// Try rfidNumber first
var driverSnapshot = hardwareDriversRef.orderByChild("rfidNumber").equalTo(rfidUID).get().await()

if (!driverSnapshot.exists()) {
    // Fallback to rfidUID
    driverSnapshot = hardwareDriversRef.orderByChild("rfidUID").equalTo(rfidUID).get().await()
}
```

## Files Modified

- `app/src/main/java/com/example/toda/service/FirebaseRealtimeDatabaseService.kt`
  - Updated `getDriverContributions()` function
  - Updated `recordCoinInsertion()` function

## Testing Instructions

1. **Test Contributions Loading:**
   - Login as a driver who has contributions
   - Navigate to the Contributions screen
   - Verify contributions are now displayed
   - Check that the summary shows correct totals

2. **Test RFID History:**
   - Go to RFID Management tab
   - Verify RFID history entries are displayed (if any exist)
   - Check that current RFID is shown correctly

3. **Test Both RFID Fields:**
   - Test with drivers who have `rfidNumber` set
   - Test with drivers who have `rfidUID` set
   - Both should work correctly now

## Debug Output

The fix adds detailed console logging:
```
=== GETTING DRIVER CONTRIBUTIONS ===
Driver ID: <driverId>
Driver RFID (rfidNumber): <value or empty>
Driver RFID (rfidUID): <value or empty>
Using RFID: <selected value>
Found X contributions for driver <driverId> (RFID: <rfid>)
```

Check Android Logcat for these messages if contributions still don't appear.

## Additional Notes

- The `observeDriverRfid()` function already had this dual-field logic implemented
- This fix brings `getDriverContributions()` in line with that standard
- No database migration needed - works with existing data
- Backward compatible with both old and new RFID field names

## Related Functions Also Checked

These functions already properly check both RFID fields:
- `observeDriverRfid()` ✓
- `observeDriverQueueStatus()` ✓ (checks by name as fallback too)

## If Issue Persists

If contributions still don't show after this fix:

1. **Check Database Structure:**
   - Verify the driver has an RFID assigned in Firebase Console
   - Check path: `drivers/{driverId}/rfidNumber` or `drivers/{driverId}/rfidUID`

2. **Check Contributions Table:**
   - Verify contributions exist in `contributions/` table
   - Check that `driverRFID` field matches the driver's actual RFID

3. **Check Logs:**
   - Run the app with Logcat open
   - Look for the debug output mentioned above
   - Check for any error messages

4. **Verify Driver ID:**
   - Ensure the driver is logged in with the correct user ID
   - The user ID should match the key in `drivers/` table

