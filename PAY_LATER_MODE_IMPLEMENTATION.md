# Pay Later Mode Implementation Guide

## Overview
This document describes the Pay Later Mode system integrated into the driver-side application. The system allows drivers to choose between two payment modes:
1. **Pay Every Trip**: Pay ₱5.00 contribution before each trip
2. **Pay Later**: Accumulate ₱5.00 per trip, pay at the end of the day

## Key Features

### 1. Payment Mode Selection
- Drivers can choose their preferred payment mode
- Modes can be switched at any time
- Default mode: "pay_every_trip"

### 2. Balance Tracking
- Automatic tracking of unpaid contributions in Pay Later mode
- Balance increases by ₱5.00 after each completed trip
- Balance visible on driver dashboard

### 3. Online Status Restrictions
- **Pay Every Trip Mode**: No restrictions, driver can always go online
- **Pay Later Mode**: 
  - Can go online during the same day they accrued the balance
  - **Cannot go online the next day** if they have unpaid balance
  - Must settle balance to go online again

### 4. Payment History
- All contributions are tracked in `payment_history` node
- Records include:
  - Trip contributions (₱5.00 per trip)
  - Manual payments
  - Timestamps
  - Associated booking IDs

## Database Structure

### Driver Fields (in `drivers/{driverId}`)
```json
{
  "paymentMode": "pay_later",           // or "pay_every_trip"
  "balance": 15.00,                     // Running balance
  "lastPaymentDate": 1730678400000,    // Timestamp of last contribution/payment
  "canGoOnline": true                   // Computed based on balance and date
}
```

### Payment History (in `payment_history/{entryId}`)
```json
{
  "driverId": "driver_1759325281790_2222",
  "bookingId": "booking_123",           // null for manual payments
  "amount": 5.00,
  "type": "TRIP_CONTRIBUTION",          // or "PAYMENT"
  "timestamp": 1730678400000
}
```

## Implementation Details

### 1. New Service: `DriverPaymentService`
**Location**: `app/src/main/java/com/example/toda/service/DriverPaymentService.kt`

**Key Methods**:
- `canDriverGoOnline(driverId)`: Check if driver can go online based on balance
- `recordTripContribution(driverId, bookingId)`: Add ₱5 to balance after trip completion
- `processPayment(driverId, amount)`: Process manual payment to clear balance
- `getDriverBalance(driverId)`: Get current balance
- `updatePaymentMode(driverId, mode)`: Change payment mode
- `getPaymentHistory(driverId)`: Retrieve payment history

### 2. Updated Models

**Driver Model** (`data/Models.kt`):
Added fields:
```kotlin
val paymentMode: String = "pay_every_trip"
val balance: Double = 0.0
val lastPaymentDate: Long = 0L
val canGoOnline: Boolean = true
```

### 3. Integration with Booking Completion

**EnhancedBookingViewModel** (`viewmodel/EnhancedBookingViewModel.kt`):
```kotlin
fun updateBookingStatusOnly(bookingId: String, status: String) {
    // When trip is completed
    if (status == "COMPLETED") {
        // Record ₱5 contribution for the driver
        paymentService.recordTripContribution(driverId, bookingId)
    }
}
```

### 4. Driver Dashboard Updates

**DriverInterface** (`ui/driver/DriverInterface.kt`):

**Added Display Elements**:
1. **Balance Warning Card** (shown when balance > 0 and can't go online):
   - Red warning with balance amount
   - Message: "Please settle your balance to go online"

2. **Payment Mode Info Card**:
   - Shows current payment mode
   - Displays current balance
   - Color-coded: Yellow for unpaid balance, Green for zero balance

3. **Payment Mode Variables**:
   ```kotlin
   var paymentMode by remember { mutableStateOf("pay_every_trip") }
   var driverBalance by remember { mutableStateOf(0.0) }
   var canGoOnline by remember { mutableStateOf(true) }
   ```

### 5. New UI Screen: `PaymentModeScreen`
**Location**: `app/src/main/java/com/example/toda/ui/driver/PaymentModeScreen.kt`

**Features**:
- Display current balance
- Payment mode selection dialog
- Pay balance button (when balance > 0)
- Information about payment modes

## User Flow

### For Pay Later Mode Drivers:

1. **During the Day**:
   - Driver completes trip → ₱5 added to balance
   - Balance: ₱5.00
   - Driver can still go online (same day)
   - Completes another trip → Balance: ₱10.00
   - Can continue working

2. **Next Day**:
   - Driver tries to go online
   - System checks: balance > 0 AND it's a new day
   - **Blocked from going online**
   - Dashboard shows red warning
   - Must pay balance to continue

3. **After Payment**:
   - Driver pays ₱10.00
   - Balance reset to ₱0.00
   - Can go online again

### For Pay Every Trip Mode Drivers:
- No balance tracking
- No online restrictions
- Contributions still recorded for history
- Can always go online

## Testing the Implementation

### Test Case 1: Pay Later Mode - Same Day
```
1. Set driver payment mode to "pay_later"
2. Complete 3 trips
3. Check balance: Should be ₱15.00
4. Verify driver can still go online
```

### Test Case 2: Pay Later Mode - Next Day Block
```
1. Driver has balance of ₱15.00 from yesterday
2. Next day arrives (simulate by changing date)
3. Try to go online
4. Expected: Blocked with warning message
```

### Test Case 3: Payment Clears Balance
```
1. Driver has ₱15.00 balance
2. Process payment of ₱15.00
3. Check balance: Should be ₱0.00
4. Verify driver can go online
```

### Test Case 4: Switch Payment Modes
```
1. Driver in "pay_every_trip" mode
2. Switch to "pay_later"
3. Complete trip
4. Verify balance increases by ₱5.00
```

## Admin Considerations

### Setting Up Drivers
Admins should set default payment mode when registering drivers:
```json
{
  "paymentMode": "pay_later",
  "balance": 0.0,
  "canGoOnline": true,
  "lastPaymentDate": 0
}
```

### Monitoring Balances
Admins can query drivers with unpaid balances:
```javascript
// Firebase query
firebase.database().ref('drivers')
  .orderByChild('balance')
  .startAt(0.01)
  .once('value')
```

### Manual Balance Adjustments
To manually adjust a driver's balance:
```javascript
firebase.database().ref('drivers/' + driverId).update({
  balance: 0,
  lastPaymentDate: Date.now(),
  canGoOnline: true
});
```

## Future Enhancements

1. **Payment Gateway Integration**: Accept online payments
2. **Payment Reminders**: SMS/Push notifications before end of day
3. **Payment Plans**: Allow partial payments
4. **Weekly/Monthly Options**: Extend pay later to weekly/monthly cycles
5. **Contribution Reports**: Detailed breakdown for drivers and admins
6. **Auto-deduction**: Integrate with driver earnings for automatic deduction

## Firebase Security Rules

Update Firebase Realtime Database rules:
```json
{
  "rules": {
    "drivers": {
      "$driverId": {
        ".write": "auth.uid == $driverId",
        "balance": {
          ".validate": "newData.isNumber() && newData.val() >= 0"
        },
        "paymentMode": {
          ".validate": "newData.val() == 'pay_every_trip' || newData.val() == 'pay_later'"
        }
      }
    },
    "payment_history": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

## Support and Troubleshooting

### Driver Cannot Go Online
**Check**:
1. Driver's balance in database
2. lastPaymentDate timestamp
3. Current date vs lastPaymentDate (different day?)
4. canGoOnline field value

**Solution**:
- If balance > 0 and new day: Driver must pay
- If balance = 0 but canGoOnline = false: Update canGoOnline to true

### Balance Not Updating After Trip
**Check**:
1. Trip status is "COMPLETED"
2. assignedDriverId is correctly set
3. DriverPaymentService is properly injected
4. Firebase write permissions

### Payment Mode Not Saving
**Check**:
1. Valid mode string ("pay_later" or "pay_every_trip")
2. Firebase permissions
3. Network connectivity

## Summary

The Pay Later Mode system provides flexibility for drivers while ensuring contributions are tracked and paid. The key mechanism is the balance check tied to the day change, preventing drivers from going online the next day without settling their balance.

**Contribution Tracking**: Every completed trip → ₱5.00 added to balance (Pay Later) or recorded in history (Pay Every Trip)

**Online Status Control**: Balance > 0 + New Day = Cannot go online

**Payment Process**: Pay balance → Balance = 0 → Can go online

This system maintains accountability while giving drivers the flexibility to pay at their convenience during the same day.

